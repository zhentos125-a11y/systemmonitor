package system.f1xal.monitor.db;

import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.metrics.MetricSnapshot;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private final SmartMonitorPlugin plugin;
    private Connection connection;

    public Database(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    public synchronized void init() {
        try {
            File file = new File(plugin.getDataFolder(), "metrics.db");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA busy_timeout=5000");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS metrics (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "ts INTEGER NOT NULL," +
                        "tps REAL," +
                        "memory_used INTEGER," +
                        "memory_max INTEGER," +
                        "players INTEGER," +
                        "chunks INTEGER," +
                        "avg_ping INTEGER," +
                        "gc_time INTEGER," +
                        "gc_count INTEGER)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_metrics_ts ON metrics(ts)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS actions_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "ts INTEGER NOT NULL," +
                        "rule TEXT," +
                        "action TEXT)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_actions_ts ON actions_log(ts)");
            }
            // Миграция: добавить недостающие столбцы, если БД старая
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE metrics ADD COLUMN avg_ping INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE metrics ADD COLUMN gc_time INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("ALTER TABLE metrics ADD COLUMN gc_count INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite init failed: " + e.getMessage());
        }
    }

    public synchronized void saveSnapshot(MetricSnapshot s) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO metrics(ts,tps,memory_used,memory_max,players,chunks,avg_ping,gc_time,gc_count) " +
                        "VALUES(?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, s.getTimestamp());
            ps.setDouble(2, s.getTps());
            ps.setLong(3, s.getMemoryUsed());
            ps.setLong(4, s.getMemoryMax());
            ps.setInt(5, s.getPlayers());
            ps.setInt(6, s.getChunks());
            ps.setInt(7, s.getAvgPing());
            ps.setLong(8, s.getGcTimeMs());
            ps.setLong(9, s.getGcCount());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("saveSnapshot: " + e.getMessage());
        }
    }

    public synchronized void logAction(String rule, String action) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO actions_log(ts,rule,action) VALUES(?,?,?)")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, rule);
            ps.setString(3, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("logAction: " + e.getMessage());
        }
    }

    public synchronized List<String> getRecentActions(int limit) {
        List<String> out = new ArrayList<>();
        if (connection == null) return out;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ts,rule,action FROM actions_log ORDER BY ts DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getLong(1) + " | " + rs.getString(2) + " → " + rs.getString(3));
        } catch (SQLException e) {
            plugin.getLogger().warning("getRecentActions: " + e.getMessage());
        }
        return out;
    }

    public synchronized List<MetricSnapshot> getRecentMetrics(int limit) {
        List<MetricSnapshot> out = new ArrayList<>();
        if (connection == null) return out;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ts,tps,memory_used,memory_max,players,chunks,avg_ping,gc_time,gc_count " +
                        "FROM metrics ORDER BY ts DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(readSnapshot(rs));
        } catch (SQLException e) {
            plugin.getLogger().warning("getRecentMetrics: " + e.getMessage());
        }
        return out;
    }

    public synchronized List<MetricSnapshot> getMetricsSince(long sinceTs) {
        List<MetricSnapshot> out = new ArrayList<>();
        if (connection == null) return out;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ts,tps,memory_used,memory_max,players,chunks,avg_ping,gc_time,gc_count " +
                        "FROM metrics WHERE ts >= ? ORDER BY ts ASC")) {
            ps.setLong(1, sinceTs);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(readSnapshot(rs));
        } catch (SQLException e) {
            plugin.getLogger().warning("getMetricsSince: " + e.getMessage());
        }
        return out;
    }

    private MetricSnapshot readSnapshot(ResultSet rs) throws SQLException {
        return new MetricSnapshot(
                rs.getLong(1),
                rs.getDouble(2),
                rs.getLong(3),
                rs.getLong(4),
                rs.getInt(5),
                rs.getInt(6),
                rs.getInt(7),
                rs.getLong(8),
                rs.getLong(9));
    }

    public synchronized int purgeOlderThan(long cutoffMs) {
        if (connection == null) return 0;
        int removed = 0;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM metrics WHERE ts < ?")) {
            ps.setLong(1, cutoffMs);
            removed += ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("purge metrics: " + e.getMessage()); }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM actions_log WHERE ts < ?")) {
            ps.setLong(1, cutoffMs);
            removed += ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("purge actions: " + e.getMessage()); }
        return removed;
    }

    public synchronized void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }
}