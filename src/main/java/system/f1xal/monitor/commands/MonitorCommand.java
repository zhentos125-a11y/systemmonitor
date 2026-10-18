package system.f1xal.monitor.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import system.f1xal.monitor.SmartMonitorPlugin;
import system.f1xal.monitor.diagnostics.ActionRule;
import system.f1xal.monitor.metrics.MetricSnapshot;
import system.f1xal.monitor.profiler.PluginProfile;
import system.f1xal.monitor.util.ChatDump;
import system.f1xal.monitor.util.CsvExporter;

import java.util.ArrayList;
import java.util.List;

public class MonitorCommand implements CommandExecutor, TabCompleter {

    private final SmartMonitorPlugin plugin;

    public MonitorCommand(SmartMonitorPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "gui" -> {
                if (!(sender instanceof Player p)) { onlyPlayers(sender); return true; }
                if (!plugin.getRoleManager().canView(p)) { noPerm(sender); return true; }
                plugin.getGuiManager().openMain(p);
            }
            case "profiler" -> handleProfiler(sender, args);
            case "actions" -> {
                if (!(sender instanceof Player p)) { onlyPlayers(sender); return true; }
                if (!plugin.getRoleManager().canView(p)) { noPerm(sender); return true; }
                plugin.getGuiManager().openActions(p);
            }
            case "graphs" -> {
                if (!(sender instanceof Player p)) { onlyPlayers(sender); return true; }
                if (!plugin.getRoleManager().canView(p)) { noPerm(sender); return true; }
                plugin.getGuiManager().openGraphs(p);
            }
            case "top" -> {
                if (!plugin.getRoleManager().isModerator(sender)) { noPerm(sender); return true; }
                if (sender instanceof Player p) plugin.getGuiManager().openTop(p);
                else sender.sendMessage("Только для игроков (GUI). Используйте /monitor dump.");
            }
            case "hud" -> {
                if (!(sender instanceof Player p)) { onlyPlayers(sender); return true; }
                plugin.getBossBarManager().toggle(p);
            }
            case "reload" -> {
                if (!plugin.getRoleManager().isAdmin(sender)) { noPerm(sender); return true; }
                plugin.getConfigManager().reload();
                plugin.getDiagnosticsManager().loadRules();
                sender.sendMessage(ChatColor.GREEN + "Конфиг перезагружен.");
            }
            case "purge" -> {
                if (!plugin.getRoleManager().isAdmin(sender)) { noPerm(sender); return true; }
                long days = plugin.getConfigManager().getRetentionDays();
                if (args.length >= 2) {
                    try { days = Long.parseLong(args[1]); } catch (Exception ignored) {}
                }
                long cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
                int removed = plugin.getDatabase().purgeOlderThan(cutoff);
                sender.sendMessage("§aУдалено записей: " + removed);
            }
            case "status" -> showStatus(sender);
            case "metrics" -> showMetrics(sender, args);
            case "dump" -> {
                if (!plugin.getRoleManager().isModerator(sender)) { noPerm(sender); return true; }
                ChatDump.dump(plugin, sender);
            }
            case "export" -> handleExport(sender, args);
            case "test-rule" -> {
                if (!plugin.getRoleManager().isAdmin(sender)) { noPerm(sender); return true; }
                if (args.length < 2) { sender.sendMessage("§c/monitor test-rule <id>"); return true; }
                plugin.getDiagnosticsManager().triggerManually(args[1], sender.getName());
                sender.sendMessage("§aПравило " + args[1] + " выполнено (если найдено).");
            }
            case "graph" -> {
                if (!(sender instanceof Player p)) { onlyPlayers(sender); return true; }
                if (!p.hasPermission("smartmonitor.graphs.map")) { noPerm(sender); return true; }
                String key = args.length >= 2 ? args[1] : "smartmonitor:tps";
                var series = plugin.getGraphAPI().getSeries(key);
                if (series == null) { p.sendMessage("§cНеизвестная серия: " + key); return true; }
                MapView view = org.bukkit.Bukkit.createMap(p.getWorld());
                plugin.getMapGraphManager().bind(view, key);
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                MapMeta meta = (MapMeta) map.getItemMeta();
                if (meta != null) { meta.setMapView(view); map.setItemMeta(meta); }
                p.getInventory().addItem(map);
                p.sendMessage("§aКарта-график для серии " + key + " выдана.");
            }
            default -> help(sender);
        }
        return true;
    }

    private void showStatus(CommandSender s) {
        MetricSnapshot m = plugin.getMetricsCollector().getLatest();
        if (m == null) { s.sendMessage("Нет данных"); return; }
        s.sendMessage(String.format(
                "§bTPS §f%.2f §7| §bMem §f%.1f%% §7| §bPing §f%d ms §7| §bPlayers §f%d §7| §bChunks §f%d",
                m.getTps(), m.getMemoryPercent(), m.getAvgPing(), m.getPlayers(), m.getChunks()));
    }

    private void showMetrics(CommandSender s, String[] args) {
        if (!plugin.getRoleManager().isDeveloper(s)) { noPerm(s); return; }

        int limit = 5;
        if (args.length >= 2) {
            try { limit = Math.max(1, Math.min(50, Integer.parseInt(args[1]))); }
            catch (Exception ignored) {}
        }
        final int finalLimit = limit;

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MetricSnapshot> list = plugin.getDatabase().getRecentMetrics(finalLimit);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                s.sendMessage("§bПоследние " + list.size() + " замеров:");
                for (MetricSnapshot m : list) {
                    s.sendMessage(String.format(
                            "§7ts=%d tps=%.2f mem=%.1f%% players=%d chunks=%d ping=%d",
                            m.getTimestamp(), m.getTps(), m.getMemoryPercent(),
                            m.getPlayers(), m.getChunks(), m.getAvgPing()));
                }
            });
        });
    }

    private void handleExport(CommandSender s, String[] args) {
        if (!plugin.getRoleManager().isAdmin(s)) { noPerm(s); return; }
        long days = 7;
        if (args.length >= 2) {
            try { days = Long.parseLong(args[1]); } catch (Exception ignored) {}
        }
        long since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
        CsvExporter.exportAsync(plugin, since, s);
    }

    private void handleProfiler(CommandSender sender, String[] args) {
        if (!plugin.getRoleManager().isDeveloper(sender)) { noPerm(sender); return; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            plugin.getProfilerManager().reset();
            sender.sendMessage(ChatColor.GREEN + "Профайлер сброшен.");
            return;
        }
        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        List<PluginProfile> all = plugin.getProfilerManager().getAll();
        all.sort((a, b) -> Long.compare(b.getNanosTotal(), a.getNanosTotal()));
        int perPage = 10, from = (page - 1) * perPage;
        if (from >= all.size()) { sender.sendMessage("Нет записей."); return; }
        sender.sendMessage(ChatColor.AQUA + "Профайлер (стр. " + page + "):");
        for (int i = from; i < Math.min(from + perPage, all.size()); i++) {
            PluginProfile p = all.get(i);
            sender.sendMessage(ChatColor.YELLOW + p.getName() + ChatColor.GRAY
                    + " avg=" + String.format("%.3f", p.getAvgMs()) + "ms"
                    + " total=" + String.format("%.2f", p.getNanosTotal() / 1_000_000.0) + "ms"
                    + " samples=" + p.getSamples()
                    + " tasks=" + p.getSchedulerTasks()
                    + " exc=" + p.getExceptions());
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.AQUA + "SmartMonitor v2.2");
        s.sendMessage("/monitor gui - главное меню");
        s.sendMessage("/monitor status - быстрый статус");
        s.sendMessage("/monitor dump - полный дамп в чат");
        s.sendMessage("/monitor metrics [n] - сырые замеры (async)");
        s.sendMessage("/monitor profiler [page|reset]");
        s.sendMessage("/monitor actions - GUI действий");
        s.sendMessage("/monitor graphs - GUI графиков");
        s.sendMessage("/monitor top - GUI топ-10 (5 минут)");
        s.sendMessage("/monitor graph [series] - выдать карту-график");
        s.sendMessage("/monitor hud - BossBar с TPS");
        s.sendMessage("/monitor export [days] - CSV (async)");
        s.sendMessage("/monitor purge [days] - очистка старых записей");
        s.sendMessage("/monitor test-rule <id> - триггер правила");
        s.sendMessage("/monitor reload - перезагрузка конфига");
    }

    private void noPerm(CommandSender s) { s.sendMessage(ChatColor.RED + "Недостаточно прав."); }
    private void onlyPlayers(CommandSender s) { s.sendMessage("Только для игроков"); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("gui", "status", "dump", "metrics", "profiler", "actions", "graphs",
                    "top", "graph", "hud", "export", "purge", "test-rule", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("profiler")) out.add("reset");
            if (args[0].equalsIgnoreCase("graph")) out.addAll(plugin.getGraphAPI().getSeriesMap().keySet());
            if (args[0].equalsIgnoreCase("test-rule")) {
                for (ActionRule r : plugin.getDiagnosticsManager().getRules()) {
                    if (r.id.startsWith(args[1])) out.add(r.id);
                }
            }
        }
        return out;
    }
}