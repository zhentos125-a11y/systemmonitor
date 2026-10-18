package system.f1xal.monitor.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;
import system.f1xal.monitor.profiler.PluginProfile;

import java.util.List;

public class ChatDump {

    public static void dump(SmartMonitorPlugin plugin, CommandSender sender) {
        MetricSnapshot s = plugin.getMetricsCollector().getLatest();
        sender.sendMessage(ChatColor.AQUA + "===== SmartMonitor dump =====");
        if (s != null) {
            sender.sendMessage(ChatColor.GRAY + "TPS: " + ChatColor.WHITE + String.format("%.2f", s.getTps()));
            sender.sendMessage(ChatColor.GRAY + "Mem: " + ChatColor.WHITE + String.format("%.1f%% (%d/%d MB)",
                    s.getMemoryPercent(), s.getMemoryUsed() / 1024 / 1024, s.getMemoryMax() / 1024 / 1024));
            sender.sendMessage(ChatColor.GRAY + "Players: " + ChatColor.WHITE + s.getPlayers());
            sender.sendMessage(ChatColor.GRAY + "Chunks: " + ChatColor.WHITE + s.getChunks());
        }
        sender.sendMessage(ChatColor.GRAY + "Профайлер (топ-5):");
        List<PluginProfile> top = plugin.getProfilerManager().getTop(5);
        for (PluginProfile p : top) {
            sender.sendMessage(ChatColor.YELLOW + "  " + p.getName()
                    + ChatColor.GRAY + " avg=" + String.format("%.3f", p.getAvgMs())
                    + "ms total=" + String.format("%.1f", p.getNanosTotal() / 1_000_000.0) + "ms"
                    + " exc=" + p.getExceptions());
        }
        sender.sendMessage(ChatColor.GRAY + "Правил активно: " + ChatColor.WHITE
                + plugin.getDiagnosticsManager().getRules().stream().filter(r -> r.enabled).count());
        sender.sendMessage(ChatColor.AQUA + "=============================");
    }
}