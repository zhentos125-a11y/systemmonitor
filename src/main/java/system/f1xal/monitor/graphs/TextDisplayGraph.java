package system.f1xal.monitor.graphs;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import system.f1xal.monitor.SmartMonitorPlugin;

public class TextDisplayGraph {

    private final SmartMonitorPlugin plugin;
    private TextDisplay display;
    private BukkitTask task;

    public TextDisplayGraph(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void spawn(Location loc, String seriesKey) {
        remove();
        if (loc == null || loc.getWorld() == null) return;
        GraphSeries s = plugin.getGraphAPI().getSeries(seriesKey);
        if (s == null) return;
        display = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0), new AxisAngle4f(),
                    new Vector3f(2f, 2f, 2f), new AxisAngle4f()));
            d.setText(render(s));
        });
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (display == null || !display.isValid()) { remove(); return; }
            display.setText(render(s));
        }, 40L, 40L);
    }

    private String render(GraphSeries s) {
        Double[] arr = s.snapshot();
        int bars = 20;
        StringBuilder sb = new StringBuilder("§b" + s.getKey() + "\n");
        int step = Math.max(1, arr.length / bars);
        double max = s.getMax();
        int printed = 0;
        for (int i = 0; i < arr.length && printed < bars; i += step, printed++) {
            double v = arr[i];
            int height = (int) Math.round((v / max) * 7);
            sb.append(bar(height)).append('\n');
        }
        Double last = s.peekLast();
        sb.append(String.format("§f%.2f / §7%.0f", last == null ? 0 : last, max));
        return sb.toString();
    }

    private String bar(int h) {
        String[] bars = {"▁","▂","▃","▄","▅","▆","▇","█"};
        h = Math.max(0, Math.min(7, h));
        return "§a" + bars[h];
    }

    public void remove() {
        if (task != null) { task.cancel(); task = null; }
        if (display != null && display.isValid()) display.remove();
        display = null;
    }
}