#!/usr/bin/env bash
# commit-all.sh — создаёт по одному коммиту на каждый файл проекта SmartMonitor v2.2
# Запуск: bash commit-all.sh
# Требует: git, чистое дерево (или запуск в пустом репо)

set -e

# ---------- настройка ----------
NAME="zhentos125-a11y"
EMAIL="zhentos125@gmail.com"
REMOTE="https://github.com/zhentos125-a11y/systemmonitor.git"

# ---------- инициализация ----------
if [ ! -d .git ]; then
  git init
  git config user.name  "$NAME"
  git config user.email "$EMAIL"
  git branch -M main
else
  echo "Git уже инициализирован, продолжаем..."
fi

# Убедимся, что всё "чисто" (иначе коммиты будут ломаться)
if [ -n "$(git status --porcelain)" ]; then
  echo "⚠  В рабочем дереве есть изменения. Сначала закоммитьте или спрячьте их."
  exit 1
fi

# ---------- helper ----------
commit_file() {
  local file="$1"
  local msg="$2"
  if [ ! -e "$file" ]; then
    echo "⏭  Пропуск (нет файла): $file"
    return
  fi
  git add "$file"
  if git diff --cached --quiet -- "$file"; then
    echo "⏭  Нечего коммитить: $file"
    return
  fi
  git commit -m "$msg" -- "$file"
  echo "✅ $file"
}

# ---------- 1. Корневые файлы ----------
commit_file "pom.xml" \
  "build: add Maven pom with Paper 1.21 API and shaded sqlite-jdbc"

commit_file ".gitignore" \
  "chore: ignore Maven target, IDE files, server runtime and local DBs"

commit_file "LICENSE" \
  "docs: add MIT license"

commit_file "README.md" \
  "docs: add comprehensive README with features, commands, permissions, config, GUI, profiler, graphs, diagnostics and build instructions"

# ---------- 2. Ресурсы ----------
commit_file "src/main/resources/plugin.yml" \
  "feat(resources): add plugin.yml with /monitor command and permission nodes"

commit_file "src/main/resources/config.yml" \
  "feat(config): add default config.yml with flush interval, retention, HUD color, command whitelist and diagnostic rules"

# ---------- 3. Ядро ----------
commit_file "src/main/java/system/f1xal/monitor/SmartMonitorPlugin.java" \
  "feat(core): add SmartMonitorPlugin entrypoint — bootstraps config, DB, metrics, profiler, graphs, diagnostics, GUI, HUD; wires PlayerQuitEvent cleanup; enforces onDisable order"

# ---------- 4. Конфиг ----------
commit_file "src/main/java/system/f1xal/monitor/config/ConfigManager.java" \
  "feat(config): add ConfigManager — loads flush interval, GUI refresh, retention days, HUD color and diagnostic rule sections"

# ---------- 5. БД ----------
commit_file "src/main/java/system/f1xal/monitor/db/Database.java" \
  "feat(db): add SQLite storage with WAL, busy_timeout, metrics and actions_log tables, ts indexes, ALTER-based migration for avg_ping/gc_time/gc_count, purge and read helpers"

# ---------- 6. Метрики ----------
commit_file "src/main/java/system/f1xal/monitor/metrics/MetricSnapshot.java" \
  "feat(metrics): add immutable MetricSnapshot (ts, tps, mem, players, chunks, ping, gc) with clamped memory percent"

commit_file "src/main/java/system/f1xal/monitor/metrics/MetricsCollector.java" \
  "feat(metrics): add MetricsCollector — sync sampling each second, ring buffer of 600 points, capped write buffer, async SQLite flush, hourly retention purge and flushSync on shutdown"

# ---------- 7. Профайлер ----------
commit_file "src/main/java/system/f1xal/monitor/profiler/PluginProfile.java" \
  "feat(profiler): add PluginProfile with nanos/samples/exceptions/schedulerTasks counters and avg ms"

commit_file "src/main/java/system/f1xal/monitor/profiler/ProfilerManager.java" \
  "feat(profiler): add ProfilerManager — reflective RegisteredListener wrapping for per-plugin CPU, scheduler-task sampling, exception counting, self-skip and periodic re-patch"

# ---------- 8. Графики ----------
commit_file "src/main/java/system/f1xal/monitor/graphs/GraphSeries.java" \
  "feat(graphs): add thread-safe GraphSeries with sample(), peekLast(), snapshot() and capped history"

commit_file "src/main/java/system/f1xal/monitor/graphs/GraphAPI.java" \
  "feat(graphs): add GraphAPI — registry of series with default tps/memory and async 1Hz sampler"

commit_file "src/main/java/system/f1xal/monitor/graphs/SignGraphManager.java" \
  "feat(graphs): add SignGraphManager — per-location task registry, safe update, pause/resume on chunk load/unload, ASCII bar rendering"

commit_file "src/main/java/system/f1xal/monitor/graphs/SignGraphListener.java" \
  "feat(graphs): add SignGraphListener — [monitor] sign creation, permission check, auto-unregister on break, pause/resume on chunk events"

commit_file "src/main/java/system/f1xal/monitor/graphs/MapGraphManager.java" \
  "feat(graphs): add MapGraphManager — binds MapView to series, tracks only own renderers, safe shutdown"

commit_file "src/main/java/system/f1xal/monitor/graphs/MapGraphListener.java" \
  "feat(graphs): add MapGraphListener — Shift+RMB empty map to create a TPS graph map"

commit_file "src/main/java/system/f1xal/monitor/graphs/MapGraphRenderer.java" \
  "feat(graphs): add MapGraphRenderer — 128x128 TPS line with Bresenham, explicit palette bytes and checkerboard background"

commit_file "src/main/java/system/f1xal/monitor/graphs/TextDisplayGraph.java" \
  "feat(graphs): add TextDisplayGraph — 3D bar graph via TextDisplay, task auto-cancel on invalidate and world null-check"

# ---------- 9. GUI ----------
commit_file "src/main/java/system/f1xal/monitor/gui/GuiManager.java" \
  "feat(gui): add GuiManager — opens menus, tracks current view, live refresh by title prefix, safe clear"

commit_file "src/main/java/system/f1xal/monitor/gui/GuiListener.java" \
  "feat(gui): add GuiListener — cancel clicks, handle main menu buttons, toggle rules, pagination, clear state on close"

commit_file "src/main/java/system/f1xal/monitor/gui/GuiPulse.java" \
  "feat(gui): add GuiPulse — animate critical metric borders in main menu"

commit_file "src/main/java/system/f1xal/monitor/gui/MainMenu.java" \
  "feat(gui): add MainMenu — server status compass, navigational buttons, reload button, filler panes"

commit_file "src/main/java/system/f1xal/monitor/gui/PluginsGui.java" \
  "feat(gui): add PluginsGui — top-10 profiled plugins with avg/total/samples/exceptions lore"

commit_file "src/main/java/system/f1xal/monitor/gui/ActionsGui.java" \
  "feat(gui): add ActionsGui — paginated rules list (45/page), toggle on click, triggers display, WeakHashMap page state"

commit_file "src/main/java/system/f1xal/monitor/gui/GraphGui.java" \
  "feat(gui): add GraphGui — list of GraphAPI series with last value, max and buffer size"

commit_file "src/main/java/system/f1xal/monitor/gui/TopGui.java" \
  "feat(gui): add TopGui — async top-10 by TPS and memory for last 5 minutes"

# ---------- 10. Диагностика ----------
commit_file "src/main/java/system/f1xal/monitor/diagnostics/ActionRule.java" \
  "feat(diagnostics): add ActionRule with metric/operator validation, sustain, cooldown and triggers counter"

commit_file "src/main/java/system/f1xal/monitor/diagnostics/ActionHistory.java" \
  "feat(diagnostics): add ActionHistory — bounded deque of (ts, rule, action, sender)"

commit_file "src/main/java/system/f1xal/monitor/diagnostics/DiagnosticsManager.java" \
  "feat(diagnostics): add DiagnosticsManager — rule evaluation each 5s, command whitelist, cooldown, manual trigger, state preservation across reload, actions.log writer"

# ---------- 11. HUD / роли / утилиты ----------
commit_file "src/main/java/system/f1xal/monitor/hud/BossBarManager.java" \
  "feat(hud): add BossBarManager — per-player toggle, configurable color, auto-remove on quit"

commit_file "src/main/java/system/f1xal/monitor/roles/RoleManager.java" \
  "feat(roles): add RoleManager — admin/developer/moderator checks and canView/canProfile/canManageActions helpers"

commit_file "src/main/java/system/f1xal/monitor/util/CsvExporter.java" \
  "feat(util): add CsvExporter — async CSV dump of metrics since given timestamp"

commit_file "src/main/java/system/f1xal/monitor/util/ChatDump.java" \
  "feat(util): add ChatDump — one-shot chat summary of metrics, top plugins and active rules"

# ---------- 12. Команды ----------
commit_file "src/main/java/system/f1xal/monitor/commands/MonitorCommand.java" \
  "feat(commands): add /monitor with gui/status/dump/metrics/profiler/actions/graphs/top/graph/hud/export/purge/test-rule/reload and tab completion"

# ---------- 13. Remote + push ----------
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "$REMOTE"
fi

echo ""
echo "🎉 Все коммиты созданы."
echo ""
git log --oneline
echo ""
echo "Чтобы отправить на GitHub:"
echo "  git push -u origin main"