package system.f1xal.monitor.diagnostics;

import java.util.ArrayDeque;
import java.util.Deque;

public class ActionHistory {
    public record Entry(long ts, String rule, String action, String sender) {}
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final int max;

    public ActionHistory(int max) { this.max = max; }

    public synchronized void add(String rule, String action, String sender) {
        entries.addFirst(new Entry(System.currentTimeMillis(), rule, action, sender));
        while (entries.size() > max) entries.removeLast();
    }

    public synchronized Deque<Entry> snapshot() { return new ArrayDeque<>(entries); }
}