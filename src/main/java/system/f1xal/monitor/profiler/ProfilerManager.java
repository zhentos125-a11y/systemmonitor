package system.f1xal.monitor.profiler;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitTask;
import system.f1xal.monitor.SmartMonitorPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilerManager {

    private final SmartMonitorPlugin plugin;
    private final Map<String, PluginProfile> profiles = new ConcurrentHashMap<>();
    private final Set<RegisteredListener> wrapped = Collections.newSetFromMap(new IdentityHashMap<>());
    private Field handlersField;
    private boolean fieldMissing = false;
    private int tick = 0;
    private volatile boolean running;

    public ProfilerManager(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public void start() {
        running = true;
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            profiles.putIfAbsent(p.getName(), new PluginProfile(p.getName()));
        }
        try {
            handlersField = HandlerList.class.getDeclaredField("handlers");
            handlersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            fieldMissing = true;
            plugin.getLogger().warning("Profiler: HandlerList.handlers not accessible. Profiler disabled.");
        }
        if (!fieldMissing) {
            for (HandlerList hl : HandlerList.getHandlerLists()) patch(hl);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        if (!running) return;
        tick++;
        if (tick % 20 == 0 && !fieldMissing) {
            for (HandlerList hl : HandlerList.getHandlerLists()) patch(hl);
            sampleSchedulers();
        }
    }

    private void sampleSchedulers() {
        Map<String, Integer> counts = new HashMap<>();
        for (BukkitTask t : Bukkit.getScheduler().getPendingTasks()) {
            Plugin owner = t.getOwner();
            if (owner == null) continue;
            counts.merge(owner.getName(), 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            profiles.computeIfAbsent(e.getKey(), PluginProfile::new).setSchedulerTasks(e.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void patch(HandlerList hl) {
        try {
            Object list = handlersField.get(hl);
            if (!(list instanceof Map<?, ?> map)) return;
            for (Object v : map.values()) {
                if (!(v instanceof RegisteredListener[] arr)) continue;
                for (int i = 0; i < arr.length; i++) {
                    RegisteredListener rl = arr[i];
                    if (rl instanceof ProfilingRegisteredListener) continue;
                    if (rl.getPlugin() != null && rl.getPlugin().getName().equals("SmartMonitor")) continue;
                    if (!wrapped.add(rl)) continue;
                    arr[i] = new ProfilingRegisteredListener(rl);
                }
            }
        } catch (Throwable ignored) {}
    }

    private class ProfilingRegisteredListener extends RegisteredListener {
        private final RegisteredListener delegate;
        private final String pluginName;

        ProfilingRegisteredListener(RegisteredListener delegate) {
            super(delegate.getListener(),
                    (EventExecutor) (listener, event) -> delegate.callEvent(event),
                    delegate.getPriority(),
                    delegate.getPlugin(),
                    delegate.isIgnoringCancelled());
            this.delegate = delegate;
            this.pluginName = delegate.getPlugin() != null ? delegate.getPlugin().getName() : "unknown";
        }

        @Override
        public void callEvent(Event event) throws EventException {
            long t0 = System.nanoTime();
            try {
                delegate.callEvent(event);
            } catch (EventException e) {
                profiles.computeIfAbsent(pluginName, PluginProfile::new).incrementExceptions();
                throw e;
            } catch (Throwable t) {
                profiles.computeIfAbsent(pluginName, PluginProfile::new).incrementExceptions();
                throw new EventException(t);
            } finally {
                profiles.computeIfAbsent(pluginName, PluginProfile::new).add(System.nanoTime() - t0);
            }
        }

        @Override public Plugin getPlugin() { return delegate.getPlugin(); }
        @Override public Listener getListener() { return delegate.getListener(); }
        @Override public EventPriority getPriority() { return delegate.getPriority(); }
        @Override public boolean isIgnoringCancelled() { return delegate.isIgnoringCancelled(); }
    }

    public void stop() { running = false; }

    public List<PluginProfile> getTop(int n) {
        List<PluginProfile> list = new ArrayList<>(profiles.values());
        list.sort((a, b) -> Long.compare(b.getNanosTotal(), a.getNanosTotal()));
        return list.size() > n ? list.subList(0, n) : list;
    }

    public List<PluginProfile> getAll() { return new ArrayList<>(profiles.values()); }
    public void reset() { profiles.values().forEach(PluginProfile::reset); }
    public Map<String, PluginProfile> getProfiles() { return profiles; }
}