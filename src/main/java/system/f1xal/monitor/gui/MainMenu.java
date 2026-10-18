package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.util.List;

public class MainMenu {

    public static final String TITLE = "§8SmartMonitor";

    public static void open(SmartMonitorPlugin plugin, Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(plugin, inv);
        p.openInventory(inv);
    }

    public static void fill(SmartMonitorPlugin plugin, Inventory inv) {
        for (int i = 0; i < 54; i++) inv.setItem(i, null);
        MetricSnapshot s = plugin.getMetricsCollector().getLatest();
        String tps = s == null ? "?" : String.format("%.2f", s.getTps());
        String mem = s == null ? "?" : String.format("%.1f%%", s.getMemoryPercent());
        String players = s == null ? "?" : String.valueOf(s.getPlayers());
        String chunks = s == null ? "?" : String.valueOf(s.getChunks());

        boolean critical = s != null && (s.getTps() < 15.0 || s.getMemoryPercent() > 85.0);
        Material compass = critical ? Material.REDSTONE_BLOCK : Material.COMPASS;

        inv.setItem(4, item(compass, "§bСервер",
                List.of("§7TPS: §f" + tps, "§7Память: §f" + mem,
                        "§7Игроков: §f" + players, "§7Чанков: §f" + chunks)));
        inv.setItem(20, item(Material.REDSTONE, "§cПрофайлер плагинов", List.of("§7Топ-10 по нагрузке")));
        inv.setItem(22, item(Material.WRITABLE_BOOK, "§eДействия", List.of("§7Правила самодиагностики")));
        inv.setItem(24, item(Material.MAP, "§aГрафики", List.of("§7TPS / память")));
        inv.setItem(29, item(Material.DIAMOND, "§bТоп-10 по метрикам", List.of("§7За последние 5 минут")));
        inv.setItem(33, item(Material.BARRIER, "§6Перезагрузить конфиг", List.of("§7Только для админа")));

        for (int j = 0; j < 54; j++) if (inv.getItem(j) == null) inv.setItem(j, filler());
    }

    static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(lore); it.setItemMeta(meta); }
        return it;
    }

    static ItemStack filler() { return item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()); }
}