# Package Directory & Class Map

- **Purpose**: Directory of all packages and classes with their distinct responsibilities and dependency constraints.
- **Audience**: Both (Human & Agent)
- **Related Source Paths**: [`app/src/main/java/id/my/nanclouder/nanhistory/`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/)
- **Last Verified Against**: 2026-11-01

---

## 1. Package Overview Table

| Package | Purpose | Permitted Dependencies |
|---|---|---|
| `activity` | Application entry points, multi-Activity navigation, hosting Compose roots | `ui`, `state`, `utils`, `config` |
| `config` | Preferences and configuration key declarations (`Config.kt`) | Android Context, primitive types |
| `db` | Room database schema, tables, DAOs, views, migrations | `utils` only. NEVER `ui`, `state`, or `service` |
| `debug` | Diagnostic screens and development tools (file previews, log inspection) | Development only; never referenced by release code paths |
| `receiver` | Broadcast receivers for activity recognition transitions and automation intents | `service`, `config`, Google Play Services |
| `service` | Background recording, data processing, and Quick Settings tile | `db`, `utils`, `config`, Android system services |
| `settings` | User settings activities, backup configurations, appearance options | `ui`, `state`, `config`, `utils` |
| `state` | UI ViewModels and custom state holders (e.g. `SelectionState`) | `db`, `settings`, `utils`, `config` |
| `ui` | Reusable Jetpack Compose components, themes, lists, screens | `state`, `utils`, `config` |
| `ui.graph` | Multi-series graph rendering, tooltips, and canvas visualization | Compose, `utils.graph` |
| `utils` | Helper classes (location, audio processing, formatting, sensors) | Android platform APIs, standard libraries |
| `utils.graph` | Graph configuration models, math expression evaluator, preset templates, preferences persistence | Pure Kotlin, Android Context, Gson |
| `worker` | Deferrable background work via WorkManager (`AutoDeleteWorker`) | `db`, `utils`, `config`, AndroidX Work |

---

## 2. Detailed Class Catalog

### `activity/`
- [`MainActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/MainActivity.kt): Primary application entry point displaying event lists and history views.
- [`EditEventActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/EditEventActivity.kt): Activity for creating or modifying events and tags.
- [`EventLocationActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/EventLocationActivity.kt): Entry point activity hosting event location map and routing to New UI or Old UI.
- `eventLocation/common/EventLocationHelpers.kt`: Shared color calculations, bearing arcs, and distance/speed utilities.
- `eventLocation/oldUi/EventLocationOldUi.kt`, `MapHistoryViewOld.kt`: Preserved legacy implementation of event location map.
- `eventLocation/ui/EventLocationScreen.kt`, `MapHistoryView.kt`, `EventLocationControls.kt`: Modular modern event location map screen with dynamic rendered viewport queries.
- [`YearlyMapActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/YearlyMapActivity.kt): Renders annual aggregate trip and location maps.
- [`TagDetailActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/TagDetailActivity.kt): Details and event filtering for specific tags.
- [`TrashActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/TrashActivity.kt): Displays soft-deleted events with options to restore or permanently delete.
- [`EncryptionKey.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/EncryptionKey.kt): Helper routines for database encryption key resolution.
- [`MapLibreTest.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/MapLibreTest.kt): Experimental test activity for MapLibre SDK (commented out in manifest).
- `eventDetail/EventDetailActivity.kt`: Full detail view for recorded events, audio playback, and attached metadata.

### `config/`
- [`Config.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/config/Config.kt): Typed configuration accessors reading from Android SharedPreferences.

### `db/`
- [`AppDatabase.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/AppDatabase.kt): Room database abstract class, singleton manager, checkpointing, and range query helpers.
- [`Tables.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt): Room entity declarations (`EventEntity`, `DayEntity`, `TagEntity`, `LocationEntity`, `EventTagCrossRef`, `DayTagCrossRef`).
- [`Dao.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Dao.kt): Data Access Object (`AppDao`) containing all database SQL queries.
- [`Views.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Views.kt): Database views (`EventsTimeWithTag`) for joined queries.
- [`Converters.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Converters.kt): Type converters for `LocalDate`, `Instant`, `List<String>`, and `Map<String, Any>`.
- [`Migrations.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Migrations.kt): Manual Room migration scripts (`1 -> 2`, `2 -> 3`, `3 -> 4`).

### `receiver/`
- [`ActivityRecognitionEngine.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/receiver/ActivityRecognitionEngine.kt): Helper object managing Play Services Activity Recognition transition subscriptions.
- [`ActivityTransitionReceiver.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/receiver/ActivityTransitionReceiver.kt): Receives activity enter/exit events (vehicle, bicycle, running, walking, still) and dispatches start/stop/still intents to `RecordService`.
- [`RecordAutomationReceiver.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/receiver/RecordAutomationReceiver.kt): Exported broadcast receiver allowing third-party tools (Tasker, Automate, Quick Tap) to start, stop, or toggle recording.

### `service/`
- [`RecordService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordService.kt): Foreground service recording audio and location breadcrumbs (`location|microphone`).
- [`DataProcessService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/DataProcessService.kt): Foreground service processing background data, exports, and imports (`dataSync`).
- [`RecordTileService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordTileService.kt): Quick Settings tile to toggle event recording.
- [`YearlyMapWorker.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/YearlyMapWorker.kt): Background processing routine for compiling yearly map breadcrumb caches.

### `worker/`
- [`AutoDeleteWorker.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/worker/AutoDeleteWorker.kt): WorkManager `CoroutineWorker` handling scheduled purging of expired soft-deleted events.

### `state/`
- [`SelectionState.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/state/SelectionState.kt): State holder managing multi-item selection flows across UI lists.
- [`EventLocationViewModel.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/state/EventLocationViewModel.kt): ViewModel managing viewport downsampling queries, debounce, cut mode, and slicing operations for event locations.
- [`YearlyMapViewModel.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/state/YearlyMapViewModel.kt): ViewModel managing dynamic Room queries across All Time, Years, and Custom Date Ranges with corridor merging and transportation stats.

### `ui/map/`
- [`YearlyMapScreen.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapScreen.kt): Stateful screen connecting `YearlyMapViewModel` to `YearlyMapContent` and `CorridorMap`.
- [`YearlyMapContent.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapContent.kt): Stateless UI composable with time-range dropdown, transportation stats, bottom sheet, and isolated map content slot.
- [`YearlyMapView.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapView.kt): Entry point delegating directly to `YearlyMapScreen`.
- [`CorridorRender.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/CorridorRender.kt): Immutable render contract containing version, GeoJSON string, and edge-to-events lookup map.
- [`CorridorColorScale.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/CorridorColorScale.kt): Pure Kotlin mathematical color and stroke width scaling utilities.
- [`CorridorGeoJsonBuilder.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/CorridorGeoJsonBuilder.kt): Pure Kotlin builder constructing GeoJSON LineString feature collections with 6-decimal precision.
- [`MapZoomUtils.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/MapZoomUtils.kt): Standardized zoom offset conversion helpers (`osmEquivalentZoom`).
- [`DateRangePickerModal.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/DateRangePickerModal.kt): Material 3 date range picker dialog for custom trip map ranges.
- [`YearlyMapMergeLogic.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapMergeLogic.kt): Pure geometry utilities for Ramer-Douglas-Peucker track simplification, polyline resampling, and corridor vertex network snapping into canonical edges.
- [`YearlyMapLogic.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapLogic.kt) & [`YearlyMapCacheManager.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/map/YearlyMapCacheManager.kt): Legacy file-based trip cache management routines.
- `ui/map/maplibre/CorridorMap.kt`: Isolated MapLibre Compose map component rendering `LineLayer` with data-driven sort keys, interpolation, and click gestures.
- `ui/map/maplibre/BaseMapStyle.kt`: MapLibre base raster style definitions (OpenStreetMap 256px raster tiles).

### `settings/`
- `SettingsActivity.kt`, `AppearanceSettingsActivity.kt`, `RecordSettingsActivity.kt`, `StorageSettingsActivity.kt`, `AutoDeleteSettingsActivity.kt`, `BackupActivity.kt`, `DeveloperOptionsActivity.kt`, `AboutActivity.kt`: Preferences management screens.

---

## 3. Open Questions & Unverified Items
- None. All packages and source files are directly mapped from the repository file tree.
