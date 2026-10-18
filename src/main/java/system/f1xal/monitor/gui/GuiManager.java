package system.f1xal.monitor.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private final SmartMonitorPlugin plugin;
    private final Map<UUID, String> openGui = new HashMap<>();
    private int taskId = -1;

    public GuiManager(SmartMonitorPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), plugin);
        startRefresh();
    }

    private void startRefresh() {
        int ticks = plugin.getConfigManager().getGuiRefreshTicks();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String cur = openGui.get(p.getUniqueId());
                if (cur == null) continue;
                String title = p.getOpenInventory().getTitle();
                if (!title.startsWith("§8")) continue;
                Inventory inv = p.getOpenInventory().getTopInventory();
                refresh(inv, p);
            }
        }, ticks, ticks).getTaskId();
    }

    public void openMain(Player p) { openGui.put(p.getUniqueId(), "main"); MainMenu.open(plugin, p); }
    public void openPlugins(Player p) { openGui.put(p.getUniqueId(), "plugins"); PluginsGui.open(plugin, p); }
    public void openActions(Player p) { openGui.put(p.getUniqueId(), "actions"); ActionsGui.open(plugin, p); }
    public void openGraphs(Player p) { openGui.put(p.getUniqueId(), "graphs"); GraphGui.open(plugin, p); }
    public void openTop(Player p) { openGui.put(p.getUniqueId(), "top"); TopGui.open(plugin, p); }

    public String getCurrent(Player p) { return openGui.get(p.getUniqueId()); }
    public void clear(Player p) { openGui.remove(p.getUniqueId()); }

    public SmartMonitorPlugin getPlugin() { return plugin; }

    public void refresh(Inventory inv, Player p) {
        String cur = openGui.get(p.getUniqueId());
        if (cur == null) return;
        switch (cur) {
            case "main" -> MainMenu.fill(plugin, inv);
            case "plugins" -> PluginsGui.fill(plugin, inv);
            case "actions" -> ActionsGui.fill(plugin, p, inv);
            case "graphs" -> GraphGui.fill(plugin, inv);
            case "top" -> TopGui.fill(plugin, inv);
        }
    }

    public void stop() { if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId); }
}