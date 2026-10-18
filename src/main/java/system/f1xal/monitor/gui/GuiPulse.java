package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.util.List;

public class GuiPulse {

    private static final Material[] PULSE = {
            Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE
    };
    private static int frame = 0;

    public static void start(SmartMonitorPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            frame = (frame + 1) % PULSE.length;
            MetricSnapshot s = plugin.getMetricsCollector().getLatest();
            if (s == null) return;
            boolean critical = s.getTps() < 15.0 || s.getMemoryPercent() > 85.0;
            if (!critical) return;
            ItemStack pulse = MainMenu.item(PULSE[frame], " ", List.of());
            for (var p : Bukkit.getOnlinePlayers()) {
                if (!p.getOpenInventory().getTitle().equals(MainMenu.TITLE)) continue;
                Inventory inv = p.getOpenInventory().getTopInventory();
                for (int i : new int[]{0, 8, 45, 53}) inv.setItem(i, pulse);
            }
        }, 10L, 10L);
    }
}