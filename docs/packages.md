# Package Directory & Class Map

- **Purpose**: Directory of all packages and classes with their distinct responsibilities and dependency constraints.
- **Audience**: Both (Human & Agent)
- **Related Source Paths**: [`app/src/main/java/id/my/nanclouder/nanhistory/`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/)
- **Last Verified Against**: 2026-11-06

---

## 1. Package Overview Table

| Package | Purpose | Permitted Dependencies |
|---|---|---|
| `activity` | Application entry points, multi-Activity navigation, hosting Compose roots | `ui`, `state`, `domain`, `di` |
| `domain.model` | Pure business entities (`HistoryEvent`, `HistoryTag`, `LocationData`, `CorridorSnapshot`) | Pure Kotlin standard library only. ZERO Android dependencies. |
| `domain.repository` | Domain ports/interfaces (`EventRepository`, `LocationRepository`, `TagRepository`, `CorridorRepository`, `AttachmentStorageRepository`, `SettingsRepository`) | Pure Kotlin standard library and `kotlinx.coroutines` only. |
| `domain.usecase` | Interactors and domain rules (`SaveEventUseCase`, `RestoreEventsUseCase`, `PermanentlyDeleteEventsUseCase`, `PurgeExpiredTrashUseCase`) | `domain.model`, `domain.repository` |
| `domain.logic` | Pure mathematical models and algorithms (Corridors, ReplayMath, ShareCanvasLogic, Graph Evaluator, Transport scoring) | Pure Kotlin standard library only. |
| `data.repository` | Implementations of domain ports (`RoomEventRepository`, `RoomLocationRepository`, `RoomTagRepository`, `RoomCorridorRepository`, `AndroidAttachmentStorageRepository`, `ConfigSettingsRepository`) | `domain`, `db`, `config`, Android platform APIs |
| `di` | Composition root (`AppContainer`) wiring dependencies | All packages (pure composition root) |
| `config` | Preferences and configuration key declarations (`Config.kt`) | Android Context, primitive types |
| `db` | Room database schema, tables, DAOs, views, migrations | `utils` only. NEVER `ui`, `state`, `domain`, or `service` |
| `state` | UI ViewModels and custom state holders (e.g. `SelectionState`, `TrashViewModel`, `EditEventViewModel`, `EventListViewModel`, `ShareMapViewModel`) | `domain`, `settings`, `utils`, `config` |
| `ui` | Reusable Jetpack Compose components, themes, lists, screens | `state`, `domain.model`, `utils`, `config` |
| `service` | Background recording, data processing, and Quick Settings tile | `domain`, `di`, `utils`, `config`, Android system services |
| `worker` | Deferrable background work via WorkManager (`AutoDeleteWorker`, `CorridorPrecomputeWorker`, `YearlyMapWorker`) | `domain`, `di`, `config`, AndroidX Work |

---

## 2. Detailed Class Catalog

### `domain/`
- `domain.model`: Pure Kotlin data models (`HistoryEvent`, `EventPoint`, `EventRange`, `HistoryTag`, `LocationData`, `Coordinate`, `LocationBounds`, `CorridorSnapshot`, `EventSelectMode`).
- `domain.repository`: Interfaces decoupling business rules from data layers (`EventRepository`, `TagRepository`, `LocationRepository`, `CorridorRepository`, `AttachmentStorageRepository`, `SettingsRepository`).
- `domain.usecase`: Focused business interactors (`SaveEventUseCase`, `RestoreEventsUseCase`, `PermanentlyDeleteEventsUseCase`, `PurgeExpiredTrashUseCase`).
- `domain.logic`: Extracted algorithms with zero platform dependencies (`CorridorLogic`, `ReplayMath`, `ShareCanvasLogic`, `domain.logic.graph.*`, `domain.logic.transport.*`).

### `data/`
- `data.repository`: Concrete repository adapters implementing domain ports (`RoomEventRepository`, `RoomTagRepository`, `RoomLocationRepository`, `RoomCorridorRepository`, `AndroidAttachmentStorageRepository`, `ConfigSettingsRepository`).

### `di/`
- [`AppContainer.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/di/AppContainer.kt): Manual dependency injection container providing single-source instantiation and dynamic DAO resolution.

### `activity/`
- [`MainActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/MainActivity.kt): Primary application entry point displaying event lists and history views.
- [`EditEventActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/EditEventActivity.kt): Activity for creating or modifying events and tags via `EditEventViewModel` and `SaveEventUseCase`.
- [`EventLocationActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/EventLocationActivity.kt): Map replay and track viewing using `EventLocationViewModel` and `LocationRepository`.
- [`YearlyMapActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/YearlyMapActivity.kt): Renders annual aggregate trip and location maps using `YearlyMapViewModel` and `CorridorRepository`.
- [`TagDetailActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/TagDetailActivity.kt): Details and event filtering for specific tags using `TagDetailViewModel` and `TagRepository`.
- [`TrashActivity.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/activity/TrashActivity.kt): Displays soft-deleted events with options to restore or permanently delete via `TrashViewModel`.
- `shareMap/ShareMapActivity.kt`: Full-screen editing studio for composing and exporting shareable map snapshots using `ShareMapViewModel` and `EventRepository`.

### `worker/` & `service/`
- `AutoDeleteWorker.kt`: Enforces 30-day trash retention policy via `PurgeExpiredTrashUseCase`.
- `CorridorPrecomputeWorker.kt`: Precomputes corridor network topology in background windows via `CorridorRepository`.
- `YearlyMapWorker.kt`: Aggregates yearly event location traces via `EventRepository`.
- `RecordService.kt`: Foreground service for recording audio and GPS location.
