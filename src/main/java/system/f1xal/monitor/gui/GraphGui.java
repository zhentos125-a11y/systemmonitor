package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.graphs.GraphSeries;

import java.util.ArrayList;
import java.util.List;

public class GraphGui {

    public static final String TITLE = "§8Графики";

    public static void open(SmartMonitorPlugin plugin, org.bukkit.entity.Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(plugin, inv);
        p.openInventory(inv);
    }

    public static void fill(SmartMonitorPlugin plugin, Inventory inv) {
        int i = 0;
        for (GraphSeries s : plugin.getGraphAPI().getSeriesMap().values()) {
            Double last = s.getHistory().peekLast();
            List<String> lore = new ArrayList<>();
            lore.add("§7Последнее: §f" + (last == null ? "?" : String.format("%.2f", last)));
            lore.add("§7Максимум: §f" + s.getMax());
            lore.add("§7Точек: §f" + s.getHistory().size());
            inv.setItem(i, MainMenu.item(Material.MAP, "§e" + s.getKey(), lore));
            i++;
        }
        for (int j = 0; j < 54; j++) if (inv.getItem(j) == null) inv.setItem(j, MainMenu.filler());
    }
}