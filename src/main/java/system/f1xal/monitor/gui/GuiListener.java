package system.f1xal.monitor.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.diagnostics.ActionRule;

public class GuiListener implements Listener {

    private final GuiManager gui;

    public GuiListener(GuiManager gui) { this.gui = gui; }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            gui.clear(p);
            ActionsGui.clear(p);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String cur = gui.getCurrent(p);
        if (cur == null) return;
        e.setCancelled(true);
        SmartMonitorPlugin plugin = gui.getPlugin();
        int slot = e.getRawSlot();

        switch (cur) {
            case "main" -> {
                if (slot == 20) gui.openPlugins(p);
                else if (slot == 22) gui.openActions(p);
                else if (slot == 24) gui.openGraphs(p);
                else if (slot == 29) gui.openTop(p);
                else if (slot == 33 && p.hasPermission("smartmonitor.admin")) {
                    plugin.getConfigManager().reload();
                    plugin.getDiagnosticsManager().loadRules();
                    p.sendMessage("§aКонфиг перезагружен.");
                }
            }
            case "plugins" -> {}
            case "actions" -> {
                int page = ActionsGui.currentPage(p);
                var rules = plugin.getDiagnosticsManager().getRules();
                int start = page * ActionsGui.PER_PAGE;
                int idx = slot;
                if (idx >= 0 && idx < ActionsGui.PER_PAGE && (start + idx) < rules.size()) {
                    ActionRule rule = rules.get(start + idx);
                    if (p.hasPermission("smartmonitor.admin")) {
                        rule.enabled = !rule.enabled;
                        p.sendMessage("§eПравило " + rule.id + " → " + (rule.enabled ? "ВКЛ" : "ВЫКЛ"));
                    }
                } else if (slot == 45) {
                    ActionsGui.prevPage(p);
                    gui.refresh(e.getInventory(), p);
                } else if (slot == 53) {
                    ActionsGui.nextPage(p);
                    gui.refresh(e.getInventory(), p);
                }
            }
            case "top", "graphs" -> {}
        }
    }
}