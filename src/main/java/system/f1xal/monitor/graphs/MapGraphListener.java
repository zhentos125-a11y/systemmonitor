package system.f1xal.monitor.graphs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import system.f1xal.monitor.SmartMonitorPlugin;

public class MapGraphListener implements Listener {

    private final SmartMonitorPlugin plugin;

    public MapGraphListener(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("RIGHT")) return;
        Player p = e.getPlayer();
        if (!p.hasPermission("smartmonitor.graphs.map")) return;
        if (!p.isSneaking()) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.MAP) return;

        MapView view = Bukkit.createMap(p.getWorld());
        view.setScale(MapView.Scale.CLOSEST);
        view.setTrackingPosition(false);
        plugin.getMapGraphManager().bind(view, "smartmonitor:tps");

        ItemStack out = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) out.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            out.setItemMeta(meta);
        }

        p.getInventory().setItemInMainHand(out);
        p.sendMessage(ChatColor.GREEN + "Карта-график создана.");
        e.setCancelled(true);
    }
}