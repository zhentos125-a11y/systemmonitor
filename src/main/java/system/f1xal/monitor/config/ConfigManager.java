package system.f1xal.monitor.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final SmartMonitorPlugin plugin;
    private int flushIntervalSeconds;
    private int guiRefreshTicks;
    private int retentionDays;
    private String hudColor;

    public ConfigManager(SmartMonitorPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        this.flushIntervalSeconds = c.getInt("flush-interval-seconds", 50);
        this.guiRefreshTicks = c.getInt("gui-refresh-ticks", 40);
        this.retentionDays = c.getInt("retention-days", 30);
        this.hudColor = c.getString("hud.color", "AUTO");
    }

    public int getFlushIntervalSeconds() { return flushIntervalSeconds; }
    public int getGuiRefreshTicks() { return guiRefreshTicks; }
    public int getRetentionDays() { return retentionDays; }
    public String getHudColor() { return hudColor; }

    public Map<String, ConfigurationSection> getRules() {
        Map<String, ConfigurationSection> map = new HashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("rules");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                map.put(key, sec.getConfigurationSection(key));
            }
        }
        return map;
    }
}