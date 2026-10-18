package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.diagnostics.ActionRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ActionsGui {

    public static final String TITLE = "§8Действия";
    public static final int PER_PAGE = 45;
    private static final Map<UUID, Integer> pages = new WeakHashMap<>();

    public static synchronized int currentPage(Player p) {
        return pages.getOrDefault(p.getUniqueId(), 0);
    }
    public static synchronized void nextPage(Player p) {
        pages.put(p.getUniqueId(), currentPage(p) + 1);
    }
    public static synchronized void prevPage(Player p) {
        pages.put(p.getUniqueId(), Math.max(0, currentPage(p) - 1));
    }
    public static synchronized void clear(Player p) {
        pages.remove(p.getUniqueId());
    }

    public static void open(SmartMonitorPlugin plugin, Player p) {
        synchronized (pages) { pages.putIfAbsent(p.getUniqueId(), 0); }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(plugin, p, inv);
        p.openInventory(inv);
    }

    public static void fill(SmartMonitorPlugin plugin, Player p, Inventory inv) {
        for (int i = 0; i < 54; i++) inv.setItem(i, null);
        List<ActionRule> rules = plugin.getDiagnosticsManager().getRules();
        int page = currentPage(p);
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < rules.size(); i++) {
            ActionRule r = rules.get(start + i);
            Material m = r.enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            List<String> lore = new ArrayList<>();
            lore.add("§7Метрика: §f" + r.metric);
            lore.add("§7Условие: §f" + r.operator + " " + r.threshold);
            lore.add("§7Длительность: §f" + r.sustainSeconds + "s");
            lore.add("§7Cooldown: §f" + r.cooldownSeconds + "s");
            lore.add("§7Сработало: §f" + r.triggers);
            lore.add("§7Статус: §f" + (r.enabled ? "ВКЛ" : "ВЫКЛ"));
            inv.setItem(i, MainMenu.item(m, "§e" + r.id, lore));
        }
        if (page > 0) inv.setItem(45, MainMenu.item(Material.ARROW, "§e← Назад", List.of()));
        if (start + PER_PAGE < rules.size())
            inv.setItem(53, MainMenu.item(Material.ARROW, "§eВперёд →", List.of()));
        for (int j = 0; j < 54; j++) if (inv.getItem(j) == null) inv.setItem(j, MainMenu.filler());
    }
}