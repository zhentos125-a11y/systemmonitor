package system.f1xal.monitor.graphs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class GraphSeries {
    private final String key;
    private final Supplier<Double> supplier;
    private final Deque<Double> history = new ArrayDeque<>();
    private final double max;
    private final int capacity;

    public GraphSeries(String key, Supplier<Double> supplier, double max, int capacity) {
        this.key = key;
        this.supplier = supplier;
        this.max = max;
        this.capacity = capacity;
    }

    public synchronized void sample() {
        try {
            double v = supplier.get();
            history.addLast(v);
            while (history.size() > capacity) history.removeFirst();
        } catch (Throwable ignored) {}
    }

    public String getKey() { return key; }
    public double getMax() { return max; }

    public synchronized Double peekLast() { return history.peekLast(); }
    public synchronized int size() { return history.size(); }
    public synchronized Double[] snapshot() { return history.toArray(new Double[0]); }

    /** Устаревший доступ, оставлен для совместимости — вернёт копию. */
    public Deque<Double> getHistory() {
        synchronized (this) { return new ArrayDeque<>(history); }
    }
}