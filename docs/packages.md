# Package Directory & Module Catalog

- **Purpose**: Directory of all Gradle modules and packages with their distinct responsibilities and dependency constraints.
- **Audience**: Both (Human & Agent)
- **Last Verified Against**: 2026-11-07

---

## 1. Module Overview Table

| Module | Purpose | Permitted Dependencies |
|---|---|---|
| `:app` | Application entry shell, Android manifest definitions, product flavors (`plain`, `public`, `private`), keystores, composition root (`AppContainer`), navigation intent wiring | All `:feature:*` and `:core:*` modules |
| `:feature:timeline` | Home screen timeline, event list, grouping, search, multi-selection, delete orchestration | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:settings` | Settings screens (general, recording, storage, experimental, about) | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:eventdetail` | Viewing and editing events, date-time pickers, tag selection, attachments | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:record` | Audio and location recording foreground service (`RecordService`), tiles, recording state management | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:map-location` | Event location replay, GPS track rendering, map view, route stats | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:yearlymap` | Annual aggregate travel maps, corridor precomputation, yearly workers | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:sharemap` | Map snapshot canvas, styling customizations, PNG/JPEG export | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:trash` | Soft-deleted event management, restore, 30-day retention worker (`AutoDeleteWorker`) | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:feature:tag` | Tag management screen, tag detail view, tag event filtering | `:core:ui`, `:core:domain`, `:core:common`, `:core:data` |
| `:core:ui` | Reusable Jetpack Compose components, themes, typography, shared dialogs (`TagEditorDialog`, `MentionModal`), base activities | `:core:domain`, `:core:common` |
| `:core:data` | Concrete implementations of domain repository ports using Room and disk storage | `:core:domain`, `:core:database`, `:core:common` |
| `:core:database` | Room entities, DAOs, schema versions, manual migrations | `:core:common` |
| `:core:domain` | Pure Kotlin business entities, repository ports, use cases, mathematical algorithms | Pure Kotlin standard library and `kotlinx.coroutines` only (ZERO Android dependencies) |
| `:core:common` | `IntentRegistry`, manual DI container interfaces, cross-feature bridges (`RecordBridge`, `DataProcessBridge`), utilities | Android base APIs, primitives |

---

## 2. Detailed Module Catalog

### Core Modules (`core/`)

#### `:core:domain`
- **Location**: `core/domain/src/main/java/id/my/nanclouder/nanhistory/domain/`
- **Packages**:
  - `model`: Pure Kotlin data models (`HistoryEvent`, `EventPoint`, `EventRange`, `HistoryTag`, `LocationData`, `Coordinate`, `LocationBounds`, `CorridorSnapshot`, `EventSelectMode`).
  - `repository`: Repository interfaces (`EventRepository`, `TagRepository`, `LocationRepository`, `CorridorRepository`, `AttachmentStorageRepository`, `SettingsRepository`).
  - `usecase`: Business interactors (`SaveEventUseCase`, `RestoreEventsUseCase`, `PermanentlyDeleteEventsUseCase`, `PurgeExpiredTrashUseCase`).
  - `logic`: Extracted algorithms with zero platform dependencies (`CorridorLogic`, `ReplayMath`, `ShareCanvasLogic`, `domain.logic.graph.*`, `domain.logic.transport.*`).

#### `:core:database`
- **Location**: `core/database/src/main/java/id/my/nanclouder/nanhistory/db/`
- **Packages**:
  - `db`: `AppDatabase`, entity definitions (`EventEntity`, `TagEntity`, `LocationEntity`, etc.), DAOs (`AppDao`), views, migrations.

#### `:core:data`
- **Location**: `core/data/src/main/java/id/my/nanclouder/nanhistory/data/`
- **Packages**:
  - `repository`: Concrete repository adapters implementing domain ports (`RoomEventRepository`, `RoomTagRepository`, `RoomLocationRepository`, `RoomCorridorRepository`, `AndroidAttachmentStorageRepository`, `ConfigSettingsRepository`).

#### `:core:common`
- **Location**: `core/common/src/main/java/id/my/nanclouder/nanhistory/core/common/`
- **Packages**:
  - `navigation`: `IntentRegistry` and navigation contracts.
  - `di`: Granular container interfaces for manual DI.
  - `bridge`: `RecordBridge`, `RecordingState`, `DataProcessBridge`.
  - `utils`: File, corridor cache (`CorridorCacheHelper`), and location metric utilities.
  - `config`: Base configurations and preference constants (`Config.kt`).

#### `:core:ui`
- **Location**: `core/ui/src/main/java/id/my/nanclouder/nanhistory/core/ui/`
- **Packages**:
  - `theme`: Compose theme, color schemes, typography, shapes.
  - `components`: Common buttons, cards, list items, search bars, modal sheets.
  - `tags`: Shared tag editor dialog (`TagEditorDialog`).
  - `ui`: Shared UI components, base activities, and mention handler (`MentionModalHandler`).

---

### Feature Modules (`feature/`)

#### `:feature:timeline`
- **Location**: `feature/timeline/src/main/java/id/my/nanclouder/nanhistory/feature/timeline/`
- **Contents**: `MainActivity`, `MainView`, `EventList`, timeline ViewModels, selection state, search filtering.

#### `:feature:settings`
- **Location**: `feature/settings/src/main/java/id/my/nanclouder/nanhistory/feature/settings/`
- **Contents**: `SettingsActivity`, `RecordSettingsActivity`, `StorageSettingsActivity`, `ExperimentalSettingsActivity`, `AboutActivity`, preference ViewModels.

#### `:feature:eventdetail`
- **Location**: `feature/eventdetail/src/main/java/id/my/nanclouder/nanhistory/feature/eventdetail/`
- **Contents**: `EditEventActivity`, `EditEventView`, `EditEventViewModel`, date-time pickers, attachment handling.

#### `:feature:record`
- **Location**: `feature/record/src/main/java/id/my/nanclouder/nanhistory/feature/record/`
- **Contents**: `RecordService`, `RecordQuickSettingsTileService`, recording state notifications, audio & location recording engine.

#### `:feature:map-location`
- **Location**: `feature/map-location/src/main/java/id/my/nanclouder/nanhistory/feature/maplocation/`
- **Contents**: `EventLocationActivity`, OSMDroid location replay, GPS track rendering, replay controls.

#### `:feature:yearlymap`
- **Location**: `feature/yearlymap/src/main/java/id/my/nanclouder/nanhistory/feature/yearlymap/`
- **Contents**: `YearlyMapActivity`, `YearlyMapWorker`, `CorridorPrecomputeWorker`, annual aggregate trip renderer.

#### `:feature:sharemap`
- **Location**: `feature/sharemap/src/main/java/id/my/nanclouder/nanhistory/feature/sharemap/`
- **Contents**: `ShareMapActivity`, map snapshot studio, canvas exporter, theme selection.

#### `:feature:trash`
- **Location**: `feature/trash/src/main/java/id/my/nanclouder/nanhistory/feature/trash/`
- **Contents**: `TrashActivity`, `TrashViewModel`, `AutoDeleteWorker` (30-day retention enforcement).

#### `:feature:tag`
- **Location**: `feature/tag/src/main/java/id/my/nanclouder/nanhistory/feature/tag/`
- **Contents**: `TagDetailActivity`, `TagDetailViewModel`, tag event list and filters.

---

### App Shell (`app/`)
- **Location**: `app/src/main/java/id/my/nanclouder/nanhistory/`
- **Contents**:
  - `NanHistoryApp`: `Application` class, initializing logging, OSMDroid user-agent, `IntentRegistry` bindings, and workers.
  - `di/AppContainer`: Central composition root instantiating all singletons, Room DAOs, and repository implementations.
  - `service/DataProcessService`: Background processing coordinator.
  - Product flavors: `plain`, `public`, `private` build configurations and keystores.
