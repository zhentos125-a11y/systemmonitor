package system.f1xal.monitor.graphs;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapGraphManager {

    private final SmartMonitorPlugin plugin;
    private final Set<Integer> managed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Integer, MapGraphRenderer> own = new ConcurrentHashMap<>();

    public MapGraphManager(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void bind(MapView view, String seriesKey) {
        GraphSeries s = plugin.getGraphAPI().getSeries(seriesKey);
        if (s == null) return;
        MapGraphRenderer old = own.remove(view.getId());
        if (old != null) view.removeRenderer(old);
        view.setScale(MapView.Scale.CLOSEST);
        view.setTrackingPosition(false);
        MapGraphRenderer r = new MapGraphRenderer(s);
        view.addRenderer(r);
        own.put(view.getId(), r);
        managed.add(view.getId());
    }

    public boolean isManaged(int mapId) { return managed.contains(mapId); }

    public boolean isGraphMap(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        if (!(item.getItemMeta() instanceof MapMeta meta)) return false;
        MapView view = meta.getMapView();
        return view != null && isManaged(view.getId());
    }

    public void shutdown() {
        managed.clear();
        own.clear();
    }
}