package system.f1xal.monitor.hud;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {

    private final SmartMonitorPlugin plugin;
    private final BossBar bar;
    private final Set<UUID> viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private int taskId = -1;

    public BossBarManager(SmartMonitorPlugin plugin) {
        this.plugin = plugin;
        this.bar = Bukkit.createBossBar("§bSmartMonitor", BarColor.GREEN, BarStyle.SOLID);
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 20L).getTaskId();
    }

    public void toggle(Player p) {
        if (viewers.remove(p.getUniqueId())) {
            bar.removePlayer(p);
            p.sendMessage("§7HUD скрыт.");
        } else {
            viewers.add(p.getUniqueId());
            bar.addPlayer(p);
            p.sendMessage("§aHUD включён.");
        }
    }

    public void removeViewer(Player p) {
        if (viewers.remove(p.getUniqueId())) bar.removePlayer(p);
    }

    private void update() {
        if (viewers.isEmpty()) return;
        MetricSnapshot s = plugin.getMetricsCollector().getLatest();
        if (s == null) return;
        double tps = s.getTps();
        bar.setProgress(Math.max(0, Math.min(1, tps / 20.0)));
        bar.setColor(resolveColor(tps));
        bar.setTitle(String.format("§bTPS §f%.2f §7| §bMem §f%.1f%% §7| §bPing §f%d ms §7| §bPlayers §f%d",
                tps, s.getMemoryPercent(), s.getAvgPing(), s.getPlayers()));
    }

    private BarColor resolveColor(double tps) {
        String cfg = plugin.getConfigManager().getHudColor();
        if (cfg != null && !cfg.equalsIgnoreCase("AUTO")) {
            try { return BarColor.valueOf(cfg.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return tps < 15 ? BarColor.RED : tps < 18 ? BarColor.YELLOW : BarColor.GREEN;
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        bar.removeAll();
        viewers.clear();
    }
}