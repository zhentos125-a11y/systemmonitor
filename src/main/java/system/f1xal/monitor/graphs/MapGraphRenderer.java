package system.f1xal.monitor.graphs;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Deque;

public class MapGraphRenderer extends MapRenderer {

    private final GraphSeries series;

    public MapGraphRenderer(GraphSeries series) {
        super(true);
        this.series = series;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                canvas.setPixel(x, y, (byte) ((x + y) % 2 == 0 ? 0 : 8));

        Deque<Double> data = series.getHistory();
        if (data.isEmpty()) return;
        Double[] arr = data.toArray(new Double[0]);
        double max = series.getMax();
        int points = Math.min(128, arr.length);
        int start = arr.length - points;
        int prevX = -1, prevY = -1;
        for (int i = 0; i < points; i++) {
            double v = arr[start + i];
            int y = 127 - (int) Math.round((v / max) * 127.0);
            y = Math.max(0, Math.min(127, y));
            int x = (int) ((i / (double) points) * 127);
            byte c = colorFor(v, max);
            canvas.setPixel(x, y, c);
            if (prevX != -1) line(canvas, prevX, prevY, x, y, c);
            prevX = x; prevY = y;
        }
    }

    private byte colorFor(double v, double max) {
        double p = v / max;
        if (p > 0.8) return (byte) 0x14;
        if (p > 0.6) return (byte) 0x16;
        if (p > 0.4) return (byte) 0x26;
        return (byte) 0x1C;
    }

    private void line(MapCanvas canvas, int x0, int y0, int x1, int y1, byte color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            canvas.setPixel(x0, y0, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}