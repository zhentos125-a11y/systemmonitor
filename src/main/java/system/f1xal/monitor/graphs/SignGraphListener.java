package system.f1xal.monitor.graphs;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import system.f1xal.monitor.SmartMonitorPlugin;

public class SignGraphListener implements Listener {

    private final SmartMonitorPlugin plugin;

    public SignGraphListener(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (e.getLine(0) == null || !e.getLine(0).equalsIgnoreCase("[monitor]")) return;
        if (!e.getPlayer().hasPermission("smartmonitor.graphs.sign")) {
            e.getPlayer().sendMessage(ChatColor.RED + "Нет прав.");
            return;
        }
        String seriesKey = e.getLine(1);
        if (seriesKey == null || seriesKey.isEmpty()) seriesKey = "smartmonitor:tps";
        GraphSeries s = plugin.getGraphAPI().getSeries(seriesKey);
        if (s == null) {
            e.getPlayer().sendMessage(ChatColor.RED + "Неизвестная серия: " + seriesKey);
            return;
        }
        e.setLine(0, ChatColor.DARK_AQUA + "[SMON]");
        e.setLine(1, ChatColor.AQUA + seriesKey);
        e.setLine(2, "...");
        e.setLine(3, "loading");
        plugin.getSignGraphManager().register(e.getBlock().getLocation(), seriesKey);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (plugin.getSignGraphManager().isManaged(e.getBlock().getLocation())) {
            plugin.getSignGraphManager().unregister(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        plugin.getSignGraphManager().pause(e.getChunk());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        plugin.getSignGraphManager().resume(e.getChunk());
    }
}