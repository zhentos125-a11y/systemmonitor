package system.f1xal.monitor.metrics;

public class MetricSnapshot {
    private final long timestamp;
    private final double tps;
    private final long memoryUsed;
    private final long memoryMax;
    private final int players;
    private final int chunks;
    private final int avgPing;
    private final long gcTimeMs;
    private final long gcCount;

    public MetricSnapshot(long timestamp, double tps, long memoryUsed, long memoryMax,
                          int players, int chunks, int avgPing, long gcTimeMs, long gcCount) {
        this.timestamp = timestamp;
        this.tps = tps;
        this.memoryUsed = memoryUsed;
        this.memoryMax = memoryMax;
        this.players = players;
        this.chunks = chunks;
        this.avgPing = avgPing;
        this.gcTimeMs = gcTimeMs;
        this.gcCount = gcCount;
    }

    public long getTimestamp() { return timestamp; }
    public double getTps() { return tps; }
    public long getMemoryUsed() { return memoryUsed; }
    public long getMemoryMax() { return memoryMax; }
    public int getPlayers() { return players; }
    public int getChunks() { return chunks; }
    public int getAvgPing() { return avgPing; }
    public long getGcTimeMs() { return gcTimeMs; }
    public long getGcCount() { return gcCount; }
    public double getMemoryPercent() {
        return memoryMax == 0 ? 0 : Math.min(100.0, (memoryUsed * 100.0) / memoryMax);
    }
}