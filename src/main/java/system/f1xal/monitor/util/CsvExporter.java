package system.f1xal.monitor.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public static void exportAsync(SmartMonitorPlugin plugin, long sinceMs, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<MetricSnapshot> list = plugin.getDatabase().getMetricsSince(sinceMs);
                File out = new File(plugin.getDataFolder(),
                        "metrics-export-" + System.currentTimeMillis() + ".csv");
                try (FileWriter fw = new FileWriter(out)) {
                    fw.write("ts,tps,memory_used,memory_max,players,chunks\n");
                    for (MetricSnapshot m : list) {
                        fw.write(m.getTimestamp() + "," + m.getTps() + "," + m.getMemoryUsed() + ","
                                + m.getMemoryMax() + "," + m.getPlayers() + "," + m.getChunks() + "\n");
                    }
                }
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§aЭкспортировано (" + list.size() + " записей): "
                                + out.getAbsolutePath()));
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§cОшибка экспорта: " + e.getMessage()));
            }
        });
    }
}