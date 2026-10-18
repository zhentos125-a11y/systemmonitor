package system.f1xal.monitor.profiler;

public class PluginProfile {
    private final String name;
    private long nanosTotal;
    private long samples;
    private long exceptions;
    private int schedulerTasks;

    public PluginProfile(String name) { this.name = name; }

    public synchronized void add(long nanos) { nanosTotal += nanos; samples++; }
    public synchronized void incrementExceptions() { exceptions++; }
    public synchronized void setSchedulerTasks(int n) { schedulerTasks = n; }

    public String getName() { return name; }
    public synchronized long getNanosTotal() { return nanosTotal; }
    public synchronized long getSamples() { return samples; }
    public synchronized long getExceptions() { return exceptions; }
    public synchronized int getSchedulerTasks() { return schedulerTasks; }
    public synchronized double getAvgMs() { return samples == 0 ? 0 : (nanosTotal / 1_000_000.0) / samples; }
    public synchronized void reset() { nanosTotal = 0; samples = 0; exceptions = 0; }
}