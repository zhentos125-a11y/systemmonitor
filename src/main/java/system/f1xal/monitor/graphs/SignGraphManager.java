package system.f1xal.monitor.graphs;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitTask;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignGraphManager {

    private final SmartMonitorPlugin plugin;
    private final Map<Location, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final Map<Location, String> seriesKey = new ConcurrentHashMap<>();
    private final Map<Location, Boolean> paused = new ConcurrentHashMap<>();

    public SignGraphManager(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void register(Location loc, String key) {
        unregister(loc);
        seriesKey.put(loc, key);
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, () -> update(loc), 20L, 20L);
        tasks.put(loc, task);
    }

    public void unregister(Location loc) {
        BukkitTask t = tasks.remove(loc);
        if (t != null) t.cancel();
        seriesKey.remove(loc);
        paused.remove(loc);
    }

    public boolean isManaged(Location loc) { return tasks.containsKey(loc); }

    public void pause(Chunk chunk) {
        for (Location loc : tasks.keySet()) {
            if (sameChunk(loc, chunk)) paused.put(loc, true);
        }
    }

    public void resume(Chunk chunk) {
        for (Location loc : tasks.keySet()) {
            if (sameChunk(loc, chunk)) paused.remove(loc);
        }
    }

    private boolean sameChunk(Location a, Chunk c) {
        return a.getWorld() != null && a.getWorld().equals(c.getWorld())
                && a.getBlockX() >> 4 == c.getX() && a.getBlockZ() >> 4 == c.getZ();
    }

    private void update(Location loc) {
        if (Boolean.TRUE.equals(paused.get(loc))) return;
        if (loc.getWorld() == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        if (!(loc.getBlock().getState() instanceof Sign sg)) {
            unregister(loc);
            return;
        }
        String key = seriesKey.get(loc);
        GraphSeries gs = key == null ? null : plugin.getGraphAPI().getSeries(key);
        if (gs == null) return;
        Double last = gs.peekLast();
        if (last == null) return;
        sg.setLine(2, ChatColor.GREEN + String.format("%.2f", last));
        sg.setLine(3, ChatColor.GRAY + bar(gs));
        sg.update(false, false);
    }

    private String bar(GraphSeries s) {
        StringBuilder sb = new StringBuilder();
        Double[] arr = s.snapshot();
        int bars = 10;
        int step = Math.max(1, arr.length / bars);
        for (int i = 0; i < arr.length && sb.length() < bars; i += step) {
            double v = arr[i] / s.getMax();
            char c = v > 0.8 ? '█' : v > 0.6 ? '▓' : v > 0.4 ? '▒' : v > 0.2 ? '░' : '·';
            sb.append(c);
        }
        return sb.toString();
    }

    public void shutdown() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        seriesKey.clear();
        paused.clear();
    }
}