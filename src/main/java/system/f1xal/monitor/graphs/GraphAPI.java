package system.f1xal.monitor.graphs;

import org.bukkit.Bukkit;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class GraphAPI {

    private final SmartMonitorPlugin plugin;
    private final Map<String, GraphSeries> series = new ConcurrentHashMap<>();
    private int taskId = -1;

    public GraphAPI(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void start() {
        registerSeries("smartmonitor:tps", () -> {
            try { return Math.min(20.0, Bukkit.getTPS()[0]); } catch (Throwable t) { return 20.0; }
        }, 20.0, 300);
        registerSeries("smartmonitor:memory", () -> {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return (used * 100.0) / rt.maxMemory();
        }, 100.0, 300);

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (GraphSeries s : series.values()) s.sample();
        }, 20L, 20L).getTaskId();
    }

    public void registerSeries(String key, Supplier<Double> supplier, double max, int capacity) {
        series.putIfAbsent(key, new GraphSeries(key, supplier, max, capacity));
    }

    public GraphSeries getSeries(String key) { return series.get(key); }
    public Map<String, GraphSeries> getSeriesMap() { return series; }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        series.clear();
    }
}