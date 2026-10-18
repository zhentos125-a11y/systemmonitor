package system.f1xal.monitor.diagnostics;

import org.bukkit.configuration.ConfigurationSection;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ActionRule {

    private static final Set<String> VALID_METRICS =
            Set.of("tps", "memory_percent", "players", "chunks");
    private static final Set<String> VALID_OPS = Set.of("<", ">", "<=", ">=");

    public static class RuleAction {
        public final String type;
        public final String value;
        public final boolean optional;
        public RuleAction(String type, String value, boolean optional) {
            this.type = type; this.value = value; this.optional = optional;
        }
    }

    public final String id;
    public boolean enabled;
    public final String metric;
    public final String operator;
    public final double threshold;
    public final long sustainSeconds;
    public final long cooldownSeconds;
    public final List<RuleAction> actions = new ArrayList<>();

    public long lastTriggerMs = 0;
    public long conditionSinceMs = 0;
    public boolean armed = true;
    public long triggers = 0;

    public ActionRule(String id, ConfigurationSection sec, SmartMonitorPlugin plugin) {
        this.id = id;
        this.enabled = sec.getBoolean("enabled", true);
        this.metric = sec.getString("metric", "tps");
        this.operator = sec.getString("operator", "<");
        this.threshold = sec.getDouble("threshold", 0);
        this.sustainSeconds = sec.getLong("sustain-seconds", 60);
        this.cooldownSeconds = sec.getLong("cooldown-seconds", 300);

        if (!VALID_METRICS.contains(metric)) {
            plugin.getLogger().warning("Rule " + id + ": unknown metric '" + metric + "', disabled.");
            this.enabled = false;
        }
        if (!VALID_OPS.contains(operator)) {
            plugin.getLogger().warning("Rule " + id + ": unknown operator '" + operator + "', disabled.");
            this.enabled = false;
        }

        List<?> list = sec.getList("actions");
        if (list != null) {
            for (Object o : list) {
                if (o instanceof java.util.Map<?, ?> m) {
                    Object t = m.get("type"), v = m.get("value"), opt = m.get("optional");
                    if (t == null || v == null) continue;
                    actions.add(new RuleAction(String.valueOf(t), String.valueOf(v),
                            opt != null && Boolean.parseBoolean(String.valueOf(opt))));
                }
            }
        }
    }

    public boolean isOnCooldown(long now) {
        return cooldownSeconds > 0 && (now - lastTriggerMs) < cooldownSeconds * 1000L;
    }
}