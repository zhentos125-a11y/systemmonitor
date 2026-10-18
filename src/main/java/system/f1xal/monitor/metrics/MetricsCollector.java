package system.f1xal.monitor.metrics;

import org.bukkit.Bukkit;
import org.bukkit.World;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MetricsCollector {

    private static final int MAX_BUFFER = 6000;
    private static final int RING = 600;

    private final SmartMonitorPlugin plugin;
    private final Deque<MetricSnapshot> buffer = new ArrayDeque<>();
    private final Deque<MetricSnapshot> ringBuffer = new ArrayDeque<>();
    private final AtomicReference<MetricSnapshot> latest = new AtomicReference<>();
    private ScheduledExecutorService scheduler;
    private volatile boolean running;
    private long lastPurge = 0;

    public MetricsCollector(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void start() {
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SmartMonitor-Metrics");
            t.setDaemon(true);
            return t;
        });
        Bukkit.getScheduler().runTaskTimer(plugin, this::sample, 20L, 20L);
        scheduler.scheduleAtFixedRate(this::flush,
                plugin.getConfigManager().getFlushIntervalSeconds(),
                plugin.getConfigManager().getFlushIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void sample() {
        try {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();
            if (max == Long.MAX_VALUE || max <= 0) max = rt.totalMemory();
            int players = Bukkit.getOnlinePlayers().size();
            int chunks = 0;
            for (World w : Bukkit.getWorlds()) chunks += w.getLoadedChunks().length;
            int ping = (int) Bukkit.getOnlinePlayers().stream()
                    .mapToInt(p -> p.getPing()).average().orElse(0);
            long gcTime = 0, gcCount = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                gcTime += gc.getCollectionTime();
                gcCount += gc.getCollectionCount();
            }
            double tps;
            try { tps = Math.min(20.0, Bukkit.getTPS()[0]); } catch (Throwable t) { tps = 20.0; }

            MetricSnapshot snap = new MetricSnapshot(System.currentTimeMillis(), tps, used, max,
                    players, chunks, ping, gcTime, gcCount);
            latest.set(snap);
            synchronized (buffer) {
                buffer.addLast(snap);
                while (buffer.size() > MAX_BUFFER) buffer.removeFirst();
            }
            synchronized (ringBuffer) {
                ringBuffer.addLast(snap);
                while (ringBuffer.size() > RING) ringBuffer.removeFirst();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("sample error: " + t.getMessage());
        }
    }

    private void flush() {
        if (!running) return;
        try {
            synchronized (buffer) {
                while (!buffer.isEmpty()) {
                    MetricSnapshot s = buffer.pollFirst();
                    if (s != null) plugin.getDatabase().saveSnapshot(s);
                }
            }
            long now = System.currentTimeMillis();
            if (now - lastPurge > 60L * 60 * 1000) {
                lastPurge = now;
                long retentionMs = plugin.getConfigManager().getRetentionDays() * 24L * 60 * 60 * 1000;
                plugin.getDatabase().purgeOlderThan(now - retentionMs);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Flush error: " + t.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdownNow();
        flushSync();
    }

    /** Явный flush из onDisable — синхронно, без scheduler. */
    public void flushSync() {
        try {
            synchronized (buffer) {
                while (!buffer.isEmpty()) {
                    MetricSnapshot s = buffer.pollFirst();
                    if (s != null) plugin.getDatabase().saveSnapshot(s);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("flushSync: " + t.getMessage());
        }
    }

    public MetricSnapshot getLatest() { return latest.get(); }

    public Deque<MetricSnapshot> getRingBuffer() {
        synchronized (ringBuffer) { return new ArrayDeque<>(ringBuffer); }
    }
}