package system.f1xal.monitor.roles;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import system.f1xal.monitor.SmartMonitorPlugin;

public class RoleManager {

    private final SmartMonitorPlugin plugin;

    public RoleManager(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public boolean isAdmin(CommandSender s) { return s.hasPermission("smartmonitor.admin"); }

    public boolean isDeveloper(CommandSender s) {
        return s.hasPermission("smartmonitor.developer") || isAdmin(s);
    }

    public boolean isModerator(CommandSender s) {
        return s.hasPermission("smartmonitor.moderator") || isDeveloper(s);
    }

    public boolean canView(Player p) { return isModerator(p); }
    public boolean canProfile(Player p) { return isDeveloper(p); }
    public boolean canManageActions(Player p) { return isAdmin(p); }
}