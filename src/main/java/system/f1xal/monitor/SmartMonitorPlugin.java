package system.f1xal.monitor;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import system.f1xal.monitor.commands.MonitorCommand;
import system.f1xal.monitor.config.ConfigManager;
import system.f1xal.monitor.db.Database;
import system.f1xal.monitor.diagnostics.DiagnosticsManager;
import system.f1xal.monitor.graphs.*;
import system.f1xal.monitor.gui.GuiManager;
import system.f1xal.monitor.gui.GuiPulse;
import system.f1xal.monitor.hud.BossBarManager;
import system.f1xal.monitor.metrics.MetricsCollector;
import system.f1xal.monitor.profiler.ProfilerManager;
import system.f1xal.monitor.roles.RoleManager;

public final class SmartMonitorPlugin extends JavaPlugin implements Listener {

    private static SmartMonitorPlugin instance;

    private ConfigManager configManager;
    private Database database;
    private MetricsCollector metricsCollector;
    private ProfilerManager profilerManager;
    private GuiManager guiManager;
    private DiagnosticsManager diagnosticsManager;
    private RoleManager roleManager;
    private GraphAPI graphAPI;
    private SignGraphManager signGraphManager;
    private MapGraphManager mapGraphManager;
    private BossBarManager bossBarManager;

    public static SmartMonitorPlugin get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.database = new Database(this);
        this.database.init();

        this.roleManager = new RoleManager(this);
        this.metricsCollector = new MetricsCollector(this);
        this.metricsCollector.start();

        this.profilerManager = new ProfilerManager(this);
        this.profilerManager.start();

        this.graphAPI = new GraphAPI(this);
        this.graphAPI.start();

        this.signGraphManager = new SignGraphManager(this);
        this.mapGraphManager = new MapGraphManager(this);
        this.bossBarManager = new BossBarManager(this);

        Bukkit.getPluginManager().registerEvents(new SignGraphListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MapGraphListener(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        this.diagnosticsManager = new DiagnosticsManager(this);
        this.diagnosticsManager.start();

        this.guiManager = new GuiManager(this);
        GuiPulse.start(this);

        MonitorCommand cmd = new MonitorCommand(this);
        if (getCommand("monitor") != null) {
            getCommand("monitor").setExecutor(cmd);
            getCommand("monitor").setTabCompleter(cmd);
        }

        getLogger().info("SmartMonitor v2.2 enabled.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (bossBarManager != null) bossBarManager.removeViewer(e.getPlayer());
        if (guiManager != null) guiManager.clear(e.getPlayer());
    }

    @Override
    public void onDisable() {
        if (diagnosticsManager != null) diagnosticsManager.stop();
        if (bossBarManager != null) bossBarManager.stop();
        if (signGraphManager != null) signGraphManager.shutdown();
        if (mapGraphManager != null) mapGraphManager.shutdown();
        if (graphAPI != null) graphAPI.stop();
        if (profilerManager != null) profilerManager.stop();
        if (guiManager != null) guiManager.stop();
        if (metricsCollector != null) metricsCollector.stop();
        if (database != null) database.close();
        getLogger().info("SmartMonitor disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public Database getDatabase() { return database; }
    public MetricsCollector getMetricsCollector() { return metricsCollector; }
    public ProfilerManager getProfilerManager() { return profilerManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public DiagnosticsManager getDiagnosticsManager() { return diagnosticsManager; }
    public RoleManager getRoleManager() { return roleManager; }
    public GraphAPI getGraphAPI() { return graphAPI; }
    public SignGraphManager getSignGraphManager() { return signGraphManager; }
    public MapGraphManager getMapGraphManager() { return mapGraphManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
}