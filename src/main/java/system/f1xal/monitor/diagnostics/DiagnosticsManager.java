package system.f1xal.monitor.diagnostics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DiagnosticsManager {

    private final SmartMonitorPlugin plugin;
    private final List<ActionRule> rules = new ArrayList<>();
    private final ActionHistory history = new ActionHistory(200);
    private final Set<String> allowedCommands = new HashSet<>();
    private final java.io.File logFile;
    private int taskId = -1;

    public DiagnosticsManager(SmartMonitorPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new java.io.File(plugin.getDataFolder(), "actions.log");
    }

    public void start() {
        loadWhitelist();
        loadRules();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::evaluate, 100L, 100L).getTaskId();
    }

    private void loadWhitelist() {
        allowedCommands.clear();
        List<String> cfg = plugin.getConfig().getStringList("allowed-commands");
        if (cfg == null || cfg.isEmpty()) {
            allowedCommands.addAll(List.of("spark", "gc", "clearlag", "tps", "lag", "paper", "save-all"));
        } else {
            cfg.forEach(c -> allowedCommands.add(c.toLowerCase()));
        }
    }

    public void loadRules() {
        Map<String, ActionRule> old = new HashMap<>();
        for (ActionRule r : rules) old.put(r.id, r);
        rules.clear();
        for (Map.Entry<String, ConfigurationSection> e : plugin.getConfigManager().getRules().entrySet()) {
            ActionRule r = new ActionRule(e.getKey(), e.getValue(), plugin);
            ActionRule prev = old.get(r.id);
            if (prev != null) {
                r.lastTriggerMs = prev.lastTriggerMs;
                r.conditionSinceMs = prev.conditionSinceMs;
                r.armed = prev.armed;
                r.triggers = prev.triggers;
            }
            rules.add(r);
        }
    }

    private void evaluate() {
        MetricSnapshot snap = plugin.getMetricsCollector().getLatest();
        if (snap == null) return;
        long now = System.currentTimeMillis();
        for (ActionRule rule : rules) {
            if (!rule.enabled) continue;
            if (rule.isOnCooldown(now)) continue;
            double value = metricValue(rule.metric, snap);
            boolean cond = compare(value, rule.operator, rule.threshold);
            if (cond) {
                if (rule.conditionSinceMs == 0) rule.conditionSinceMs = now;
                long elapsed = (now - rule.conditionSinceMs) / 1000;
                if (rule.armed && elapsed >= rule.sustainSeconds) {
                    fire(rule);
                    rule.armed = false;
                    rule.lastTriggerMs = now;
                    rule.triggers++;
                }
            } else {
                rule.conditionSinceMs = 0;
                rule.armed = true;
            }
        }
    }

    private double metricValue(String metric, MetricSnapshot s) {
        return switch (metric) {
            case "tps" -> s.getTps();
            case "memory_percent" -> s.getMemoryPercent();
            case "players" -> s.getPlayers();
            case "chunks" -> s.getChunks();
            default -> 0;
        };
    }

    private boolean compare(double v, String op, double t) {
        return switch (op) {
            case "<" -> v < t; case ">" -> v > t; case "<=" -> v <= t; case ">=" -> v >= t;
            default -> false;
        };
    }

    private void fire(ActionRule rule) {
        for (ActionRule.RuleAction a : rule.actions) executeAction(rule.id, a, "auto");
    }

    public void executeAction(String ruleId, ActionRule.RuleAction a, String sender) {
        String desc = a.type + ": " + a.value;
        if ("COMMAND".equalsIgnoreCase(a.type)) {
            String cmd = a.value.startsWith("/") ? a.value.substring(1) : a.value;
            String name = cmd.split(" ")[0].toLowerCase();
            if (!allowedCommands.contains(name)) {
                log(ruleId, "[blocked] " + desc, sender);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            log(ruleId, desc, sender);
        } else if ("BROADCAST".equalsIgnoreCase(a.type)) {
            String msg = ChatColor.translateAlternateColorCodes('&', a.value);
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(msg));
            log(ruleId, "BROADCAST: " + a.value, sender);
        }
        plugin.getDatabase().logAction(ruleId, desc);
    }

    public void triggerManually(String ruleId, String sender) {
        for (ActionRule r : rules) {
            if (r.id.equals(ruleId)) {
                fire(r);
                r.lastTriggerMs = System.currentTimeMillis();
                r.triggers++;
                return;
            }
        }
    }

    private void log(String rule, String action, String sender) {
        history.add(rule, action, sender);
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(System.currentTimeMillis() + " | " + sender + " | " + rule + " | " + action + "\n");
        } catch (IOException e) { plugin.getLogger().warning("actions.log: " + e.getMessage()); }
    }

    public void stop() { if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId); }
    public List<ActionRule> getRules() { return rules; }
    public ActionHistory getHistory() { return history; }
}