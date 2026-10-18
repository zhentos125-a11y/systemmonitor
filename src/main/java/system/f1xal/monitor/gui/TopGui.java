package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopGui {

    public static final String TITLE = "§8Топ-10 (5 минут)";

    public static void open(SmartMonitorPlugin plugin, org.bukkit.entity.Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++) inv.setItem(i, MainMenu.filler());
        p.openInventory(inv);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MetricSnapshot> recent = plugin.getDatabase()
                    .getMetricsSince(System.currentTimeMillis() - 5 * 60 * 1000);
            Bukkit.getScheduler().runTask(plugin, () -> fillInto(inv, recent));
        });
    }

    public static void fill(SmartMonitorPlugin plugin, Inventory inv) {
        // используется GuiManager.refresh — просто перерисовываем placeholder,
        // чтобы не блокировать main-thread
    }

    private static void fillInto(Inventory inv, List<MetricSnapshot> recent) {
        for (int i = 0; i < 54; i++) inv.setItem(i, MainMenu.filler());
        if (recent.isEmpty()) {
            inv.setItem(22, MainMenu.item(Material.PAPER, "§7Нет данных", List.of()));
            return;
        }
        List<MetricSnapshot> byTps = new ArrayList<>(recent);
        byTps.sort(Comparator.comparingDouble(MetricSnapshot::getTps));
        for (int i = 0; i < Math.min(10, byTps.size()); i++) {
            MetricSnapshot m = byTps.get(i);
            inv.setItem(i, MainMenu.item(Material.REDSTONE,
                    "§c#" + (i + 1) + " TPS " + String.format("%.2f", m.getTps()),
                    List.of("§7Время: " + new java.util.Date(m.getTimestamp()),
                            "§7Mem: " + String.format("%.1f%%", m.getMemoryPercent()),
                            "§7Ping: " + m.getAvgPing() + "ms")));
        }
        List<MetricSnapshot> byMem = new ArrayList<>(recent);
        byMem.sort(Comparator.comparingDouble(MetricSnapshot::getMemoryPercent).reversed());
        for (int i = 0; i < Math.min(10, byMem.size()); i++) {
            MetricSnapshot m = byMem.get(i);
            inv.setItem(27 + i, MainMenu.item(Material.GOLD_INGOT,
                    "§6#" + (i + 1) + " Mem " + String.format("%.1f%%", m.getMemoryPercent()),
                    List.of("§7TPS: " + String.format("%.2f", m.getTps()),
                            "§7Время: " + new java.util.Date(m.getTimestamp()))));
        }
    }
}