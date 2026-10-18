package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.profiler.PluginProfile;

import java.util.ArrayList;
import java.util.List;

public class PluginsGui {

    public static final String TITLE = "§8Профайлер плагинов";

    public static void open(SmartMonitorPlugin plugin, org.bukkit.entity.Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(plugin, inv);
        p.openInventory(inv);
    }

    public static void fill(SmartMonitorPlugin plugin, Inventory inv) {
        List<PluginProfile> top = plugin.getProfilerManager().getTop(10);
        int i = 0;
        for (PluginProfile pp : top) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Сэмплов: §f" + pp.getSamples());
            lore.add("§7Среднее: §f" + String.format("%.3f", pp.getAvgMs()) + " ms");
            lore.add("§7Всего: §f" + String.format("%.2f", pp.getNanosTotal() / 1_000_000.0) + " ms");
            inv.setItem(i, MainMenu.item(Material.PAPER, "§e" + pp.getName(), lore));
            i++;
        }
        for (int j = 0; j < 54; j++) if (inv.getItem(j) == null) inv.setItem(j, MainMenu.filler());
    }
}