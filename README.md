# SmartMonitor v2.2

> Умный мониторинг Paper-сервера: TPS, память, профилировщик плагинов, внутриигровые графики, самодиагностика с автоматическими действиями и HUD.

---

## 📋 Содержание

- [Возможности](#-возможности)
- [Установка](#-установка)
- [Команды](#-команды)
- [Права](#-права)
- [Конфигурация](#-конфигурация)
- [GUI](#-gui)
- [Профилировщик](#-профилировщик)
- [Графики](#-графики)
- [Самодиагностика](#-самодиагностика)
- [HUD (BossBar)](#-hud-bossbar)
- [Хранение данных](#-хранение-данных)
- [Внутреннее устройство](#-внутреннее-устройство)
- [Сборка](#-сборка)
- [Структура](#-структура)
- [Лицензия](#-лицензия)

---

## Возможности

Полный список того, что умеет SmartMonitor v2.2.

### Метрики

- **TPS** — три значения (1m / 5m / 15m) через `Bukkit.getTPS()`, с защитой от `ArrayIndexOutOfBounds` и `NullPointerException`.
- **Память** — used/max в байтах, процент использования, защита от `Long.MAX_VALUE` у `Runtime.maxMemory()`.
- **Игроки** — `Bukkit.getOnlinePlayers().size()`.
- **Чанки** — сумма `World.getLoadedChunks().length` по всем мирам.
- **Средний пинг** — `IntStream.average()` по `Player.getPing()`, `0` если игроков нет.
- **GC** — суммарные `collectionTime` и `collectionCount` по всем `GarbageCollectorMXBean`.
- **Семплирование** — раз в секунду (`runTaskTimer(20L, 20L)`).
- **Кольцевой буфер** — 600 последних снапшотов в памяти (~10 минут при 1/сек).
- **Буфер записи** — до 6000 снапшотов (защита от роста при ошибках flush).
- **Тик метрики не создаёт** — всё на main-thread, но операции O(1), без запросов к БД.

### Профилировщик плагинов

- **Per-listener нагрузка** — обёртка каждого `RegisteredListener` через рефлексию к `HandlerList.handlers`.
- **Точность** — `System.nanoTime()` вокруг `callEvent()`, обёртка добавляет ~50 нс на вызов.
- **Счётчик exceptions** — ловит `EventException` и `Throwable` внутри обёртки, инкрементирует счётчик плагина, пробрасывает дальше.
- **Scheduler-таски** — количество активных `BukkitTask` по `getPendingTasks()`, раз в секунду.
- **Пропуск себя** — обёртка **не** патчит `SmartMonitor`, чтобы не мерять себя.
- **Сброс** — `/monitor profiler reset` обнуляет `nanosTotal`, `samples`, `exceptions`.
- **Топ-10** — в GUI «Профайлер» и `/monitor profiler [page]`.
- **Метрики** — `avg_ms` (среднее на вызов), `total_ms`, `samples`, `tasks`, `exc`.
- **Graceful fallback** — если поле `handlers` переименуют в новой версии Paper, логгирует WARN и не падает.
- **Периодический re-patch** — раз в секунду обходит все `HandlerList` и оборачивает новые `RegisteredListener`, защита от дублей через `IdentityHashMap`.

### GUI

- **Главное меню** (54 слота):
  - Статус сервера (TPS, память, игроки, чанки) — компас или `REDSTONE_BLOCK` при критике.
  - Кнопка «Профайлер плагинов» (слот 20).
  - Кнопка «Действия» (слот 22).
  - Кнопка «Графики» (слот 24).
  - Кнопка «Топ-10» (слот 29).
  - Кнопка «Reload конфига» (слот 33, только для админа).
- **Профайлер** — топ-10 плагинов: `samples`, `avg_ms`, `total_ms`, `exceptions`.
- **Действия** — список правил по 45 на страницу:
  - Материал `LIME_DYE` (вкл) / `GRAY_DYE` (выкл).
  - Клик — переключение (только админ).
  - Lore: метрика, условие, sustain, cooldown, triggers, статус.
  - Стрелки ←/→ для пагинации.
- **Графики** — список серий: последнее значение, максимум, размер буфера.
- **Топ-10** — топ-5 по TPS и топ-5 по памяти за 5 минут, async-загрузка из SQLite, не блокирует main-thread.
- **Live-refresh** — каждые `gui-refresh-ticks` тиков (по умолчанию 40 = 2 секунды) все открытые GUI перерисовываются.
- **Пульсация** — при TPS < 15 или памяти > 85% рамки главного меню (слоты 0, 8, 45, 53) мигают цветами `RED/ORANGE/YELLOW/LIME`.
- **Закрытие** — при `InventoryCloseEvent` очищаются `openGui`, `pages`, чтобы не было утечек.
- **Пагинация Actions** — сбрасывается при закрытии.

### Графики

#### Таблички (Sign)

- Формат: строка 0 = `[monitor]`, строка 1 = ключ серии (например, `smartmonitor:tps`).
- Автоматически привязывается к `GraphAPI`-серии.
- Обновление раз в секунду.
- Строка 2 — текущее значение (`%.2f`).
- Строка 3 — ASCII-бар `█▓▒░·` на 10 символов.
- **Автоудаление при поломке** — `BlockBreakEvent` → `unregister`.
- **Pause при выгрузке чанка** — `ChunkUnloadEvent` → `pause`.
- **Resume при загрузке** — `ChunkLoadEvent` → `resume`.
- **Защита от NPE** — проверка `loc.getWorld() != null` и `isChunkLoaded`.
- **Защита от force-render** — `sign.update(false, false)`, не дёргает физику.
- **Права** — `smartmonitor.graphs.sign`.

#### Карты (MapView)

- **Создание** — Shift+ПКМ пустой картой (`MAP` → `FILLED_MAP`).
- **Или** — `/monitor graph smartmonitor:memory`.
- **Привязка** — к `MapView`, а не ко всем картам мира.
- **Управление рендерами** — хранит свой `MapGraphRenderer` в `Map<Integer, MapGraphRenderer>`, не сносит чужие рендеры.
- **Цветовая шкала** — явные байты палитры: `0x14` (зелёный, > 80%), `0x16` (жёлтый, > 60%), `0x26` (оранжевый, > 40%), `0x1C` (красный).
- **Отрисовка** — 128×128 пикселей, шахматный фон, линия через `Bresenham`.
- **Права** — `smartmonitor.graphs.map`.

#### TextDisplay (3D)

- **Создание** — `TextDisplayGraph.spawn(location, seriesKey)`.
- **Формат** — 20 строк-баров `▁▂▃▄▅▆▇█` + текущее значение.
- **Обновление** — раз в 2 секунды.
- **Автоочистка** — таск отменяется при `remove()` или `isValid() == false`.
- **Размер** — `Transformation` с `Vector3f(2, 2, 2)`, `Billboard.CENTER`.

#### Свои серии

```java
plugin.getGraphAPI().registerSeries(
    "myplugin:my_metric",
    () -> getMyMetric(),
    100.0,  // максимум
    300     // точек (5 минут при семпле 1/сек)
);

#### Структура
systemmonitor/
├── pom.xml
├── README.md
└── src/main/
    ├── java/system/f1xal/monitor/
    │   ├── SmartMonitorPlugin.java
    │   ├── commands/
    │   │   └── MonitorCommand.java
    │   ├── config/
    │   │   └── ConfigManager.java
    │   ├── db/
    │   │   └── Database.java
    │   ├── metrics/
    │   │   ├── MetricsCollector.java
    │   │   └── MetricSnapshot.java
    │   ├── profiler/
    │   │   ├── ProfilerManager.java
    │   │   └── PluginProfile.java
    │   ├── graphs/
    │   │   ├── GraphAPI.java
    │   │   ├── GraphSeries.java
    │   │   ├── SignGraphManager.java
    │   │   ├── SignGraphListener.java
    │   │   ├── MapGraphManager.java
    │   │   ├── MapGraphListener.java
    │   │   ├── MapGraphRenderer.java
    │   │   └── TextDisplayGraph.java
    │   ├── gui/
    │   │   ├── GuiManager.java
    │   │   ├── GuiListener.java
    │   │   ├── GuiPulse.java
    │   │   ├── MainMenu.java
    │   │   ├── PluginsGui.java
    │   │   ├── ActionsGui.java
    │   │   ├── GraphGui.java
    │   │   └── TopGui.java
    │   ├── diagnostics/
    │   │   ├── DiagnosticsManager.java
    │   │   ├── ActionRule.java
    │   │   └── ActionHistory.java
    │   ├── hud/
    │   │   └── BossBarManager.java
    │   ├── roles/
    │   │   └── RoleManager.java
    │   └── util/
    │       ├── ChatDump.java
    │       └── CsvExporter.java
    └── resources/
        ├── plugin.yml
        └── config.yml