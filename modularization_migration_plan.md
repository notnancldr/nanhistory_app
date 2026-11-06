# NanHistory Multi-Module Architecture Migration Plan (Feature + Core)

## 1. Executive Summary & Goals

NanHistory is currently structured as a single `:app` module organized via Clean Architecture layers (`activity` → `ui` → `state` → `domain` → `data` / `platform`, wired with manual DI via `AppContainer`).

This migration plan defines the step-by-step roadmap to transition NanHistory into an idiomatic Android **Feature + Core Multi-Module Architecture**.

### Key Architectural Decisions (Aligned via Grill-Me Session)
1. **Rollout Strategy**: Foundational Core First — extract foundational core modules (`:core:domain`, `:core:database`, `:core:data`, `:core:common`, `:core:ui`) before extracting feature modules.
2. **`:core:domain` Technology**: Pure Kotlin JVM module (`plugins { java-library; kotlin("jvm") }`), strictly enforcing zero Android dependencies at the compiler level.
3. **Inter-Feature Navigation**: Multi-Activity Intent Navigation preserved — decoupled across feature modules via an `IntentRegistry` / navigation contracts interface defined in `:core:common` and implemented in `:app`.
4. **Manual Dependency Injection**: Centralized interfaces in `:core:common` (`CoreAppContainerProvider`, feature container interfaces) implemented by `AppContainer` in `:app`, avoiding circular module dependencies and avoiding third-party DI frameworks (per `AGENTS.md`).
5. **Security & Product Flavors**: All keystores, product flavors (`plain`, `public`, `private`), and encryption keys remain exclusively managed in the `:app` root orchestrator.
6. **Git Commit Rule**: All migration work is committed to the dedicated branch `ai/modularization-refactor` (branched from `ai/architecture-refactor`). Make a clean, standalone Git commit on this branch upon the successful completion and verification of each phase using Conventional Commits (`type(scope): subject`).

---

## 2. Target Module Topology & Dependency Graph

```mermaid
flowchart TD
    subgraph AppShell[Application Shell]
        App[":app (Application, Manifest, Flavors, AppContainer, NavigationRegistry)"]
    end

    subgraph Features[Feature Modules (Android Libraries)]
        F_Timeline[":feature:timeline"]
        F_Detail[":feature:eventdetail"]
        F_Location[":feature:map-location"]
        F_Yearly[":feature:yearlymap"]
        F_Share[":feature:sharemap"]
        F_Trash[":feature:trash"]
        F_Tag[":feature:tag"]
        F_Record[":feature:record"]
        F_Settings[":feature:settings"]
    end

    subgraph Core[Core Modules]
        C_UI[":core:ui (Design System, Themes, Common Composables)"]
        C_Common[":core:common (Intent Contracts, DI Interfaces, Common Utils)"]
        C_Data[":core:data (Repository Implementations)"]
        C_DB[":core:database (Room, DAOs, Migrations)"]
        C_Domain[":core:domain (Pure Kotlin: Models, Ports, UseCases, Logic)"]
    end

    App --> Features
    App --> Core

    Features --> C_Domain
    Features --> C_UI
    Features --> C_Common

    C_UI --> C_Domain
    C_UI --> C_Common

    C_Data --> C_Domain
    C_Data --> C_DB
    C_Data --> C_Common

    C_DB --> C_Domain
```

### Module Responsibilities & Build Configuration

| Module | Type | Responsibilities | Key Dependencies |
|---|---|---|---|
| **`:app`** | `com.android.application` | Flavor configurations (`plain`, `public`, `private`), Manifest registration, Application entry point (`NanHistoryApplication`), Composition root (`AppContainer`), Navigation routing | All `:feature:*` and `:core:*` |
| **`:core:domain`** | `java-library` + `kotlin("jvm")` | Pure domain entities (`HistoryEvent`, `LocationData`), repository interfaces (`EventRepository`), use cases (`SaveEventUseCase`), pure algorithms (`CorridorLogic`, `ReplayMath`, `ShareCanvasLogic`, graph & transport heuristics) | Pure Kotlin stdlib, `kotlinx.coroutines-core` (Zero Android) |
| **`:core:database`** | `com.android.library` | Room database (`AppDatabase`), entity tables, DAOs (`AppDao`, `CorridorNetworkDao`), views, converters, and Room migrations | `:core:domain`, Room Runtime + KSP |
| **`:core:data`** | `com.android.library` | Concrete repository implementations (`RoomEventRepository`, `RoomLocationRepository`, `RoomTagRepository`, `ConfigSettingsRepository`) | `:core:domain`, `:core:database`, `:core:common` |
| **`:core:common`** | `com.android.library` | Shared navigation interfaces (`IntentRegistry`), DI contracts (`CoreContainer`), common helpers (SAF, encryption contracts, notification channel helpers) | `:core:domain`, AndroidX Core |
| **`:core:ui`** | `com.android.library` (Compose) | Design system, M3 themes, shared widgets (`SearchAppBar`, `CodeBlockCard`, `FormattedText`, markdown renderers, color pickers) | `:core:domain`, `:core:common`, Compose BOM, Material3 |
| **`:feature:trash`** | `com.android.library` (Compose) | Soft-deleted events list, restore/purge flows (`TrashActivity`, `TrashViewModel`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:tag`** | `com.android.library` (Compose) | Tag details, taxonomy management, tag-filtered history (`TagDetailActivity`, `TagDetailViewModel`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:eventdetail`** | `com.android.library` (Compose) | Event inspection, trip telemetry graphs, audio playback, edit event flow (`EventDetailActivity`, `EditEventActivity`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:map-location`**| `com.android.library` (Compose) | GPS track replay and map view (`EventLocationActivity`, `EventLocationViewModel`, `ReplayController`) | `:core:domain`, `:core:ui`, `:core:common`, OSMDroid |
| **`:feature:yearlymap`**   | `com.android.library` (Compose) | Annual topological corridor rendering (`YearlyMapActivity`, `YearlyMapViewModel`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:sharemap`**    | `com.android.library` (Compose) | Map canvas snapshotting and layout export studio (`ShareMapActivity`, `ShareMapViewModel`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:timeline`**    | `com.android.library` (Compose) | Main event feed, multi-select, day grouping, search (`MainActivity`, `EventListViewModel`, `MainView`) | `:core:domain`, `:core:ui`, `:core:common` |
| **`:feature:record`**      | `com.android.library` | Foreground recording service (`RecordService`), Quick Settings tile (`RecordTileService`), sensor heuristics | `:core:domain`, `:core:common`, Google Play Services Location |
| **`:feature:settings`**    | `com.android.library` (Compose) | Settings screens, backup & export flows, developer options (`SettingsActivity`, `BackupActivity`) | `:core:domain`, `:core:ui`, `:core:common` |

---

## 3. Phased Implementation Roadmap

> [!IMPORTANT]
> **Commit Policy**: All commits go to the `ai/modularization-refactor` branch. A clean Git commit MUST be made upon the completion of every phase (and sub-phase where indicated), formatted with Conventional Commits (`type(scope): subject`). Commits only occur after all verification gates (multi-flavor debug builds, unit tests, and release lint) pass with code 0.

### Phase 1: Build Infrastructure & Multi-Module Setup
1. **Branch Setup**:
   - Create and check out the working branch: `git checkout -b ai/modularization-refactor`
   - Every phase commit below is made on this branch; do not commit migration work directly to the main branch.
2. **Gradle Setup**:
   - Register new modules in `settings.gradle.kts`.
   - Configure shared build configuration / convention logic for JVM libraries and Android libraries.
3. **Create `:core:common`**:
   - Define `CoreContainer` interface.
   - Define `NavigationNavigator` / `IntentRegistry` contract for inter-feature navigation.
4. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
5. **Commit Phase 1**: `chore(build): setup multi-module build infrastructure and :core:common`

### Phase 2: Foundational Core Extraction
1. **Extract `:core:domain`**:
   - Create pure Kotlin JVM library (`java-library` plugin).
   - Move `domain/model/`, `domain/repository/`, `domain/usecase/`, and `domain/logic/` into `:core:domain`.
   - Update `app/build.gradle.kts` to implement `:core:domain`.
   - Update `DomainArchitectureTest` to enforce zero Android imports.
2. **Extract `:core:database`**:
   - Create Android library module.
   - Move `db/` (Room database, entities, DAOs, migrations, schema export directory) to `:core:database`.
   - Preserve existing exported Room JSON schemas.
3. **Extract `:core:data`**:
   - Create Android library module.
   - Move `data/repository/` (`RoomEventRepository`, `RoomLocationRepository`, `RoomTagRepository`, `RoomCorridorRepository`, `ConfigSettingsRepository`) to `:core:data`.
4. **Extract `:core:ui`**:
   - Create Android Compose library module.
   - Move shared theme (`ui/theme/`), typography, and reusable components (`SearchAppBar`, `CodeBlockCard`, `FormattedText`, markdown renderers).
5. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
6. **Commit Phase 2**: `refactor(core): extract foundational :core:domain, :core:database, :core:data, and :core:ui modules`

### Phase 3: Pilot Feature Extraction (`:feature:trash` & `:feature:tag`)
1. **Extract `:feature:trash`**:
   - Move `TrashActivity.kt`, `TrashViewModel.kt`, and related UI composables.
   - Wire dependency to `:core:domain`, `:core:ui`, `:core:common`.
   - Register `TrashActivity` launcher intent in `:core:common` navigation contract.
2. **Extract `:feature:tag`**:
   - Move `TagDetailActivity.kt`, `TagDetailViewModel.kt`, and tag UI components.
   - Wire dependency to `:core:domain`, `:core:ui`, `:core:common`.
3. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
4. **Commit Phase 3**: `refactor(feature): extract pilot feature modules :feature:trash and :feature:tag`

### Phase 4: Map & Visualization Features (`:feature:map-location`, `:feature:yearlymap`, `:feature:sharemap`)
1. **Extract `:feature:map-location`**:
   - Move `EventLocationActivity.kt`, `EventLocationViewModel.kt`, `ReplayController.kt`, and UI components.
   - Wire OSMDroid dependencies.
2. **Extract `:feature:yearlymap`**:
   - Move `YearlyMapActivity.kt`, `YearlyMapViewModel.kt`, and corridor map screens.
3. **Extract `:feature:sharemap`**:
   - Move `ShareMapActivity.kt`, `ShareMapViewModel.kt`, and share canvas UI.
4. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
5. **Commit Phase 4**: `refactor(feature): extract map visualization modules :feature:map-location, :feature:yearlymap, and :feature:sharemap`

### Phase 5: Event Detail, Edit & Recording (`:feature:eventdetail`, `:feature:record`)
1. **Extract `:feature:eventdetail`**:
   - Move `EventDetailActivity.kt`, `EditEventActivity.kt`, and `EditEventViewModel.kt`.
2. **Extract `:feature:record`**:
   - Move `RecordService.kt`, `RecordTileService.kt`, and background recording logic.
3. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
4. **Commit Phase 5**: `refactor(feature): extract :feature:eventdetail and :feature:record modules`

### Phase 6: Timeline & Settings (`:feature:timeline`, `:feature:settings`)
1. **Extract `:feature:timeline`**:
   - Move `MainActivity.kt`, `MainView.kt`, `EventListViewModel.kt`, and list components.
2. **Extract `:feature:settings`**:
   - Move `SettingsActivity.kt`, `BackupActivity.kt`, and sub-settings activities.
3. **Gate Verification**:
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew testPlainDebugUnitTest`
   - `./gradlew lintVitalPlainRelease`
4. **Commit Phase 6**: `refactor(feature): extract :feature:timeline and :feature:settings modules`

### Phase 7: App Shell Consolidation & Final Architecture Verification
1. Verify `:app` acts strictly as an application coordinator (Manifest, Application class, Flavors, `AppContainer`).
2. Run full test suite across all flavors:
   - `./gradlew test`
   - `./gradlew assemblePlainDebug assemblePublicDebug assemblePrivateDebug`
   - `./gradlew lintVitalPlainRelease`
3. Update `AGENTS.md`, `docs/architecture.md`, and `docs/packages.md`.
4. **Commit Phase 7**: `docs(arch): update architecture and packages documentation for feature+core multi-module`

---

## 4. Risk Mitigation & Guardrails

- **Build Performance & Cacheability**: Independent feature modules allow parallel compilation and fine-grained Gradle task caching.
- **Room Migration Preservation**: Exported schema directory and Room migration test helper remain locked and verified.
- **Flavor & Encryption Isolation**: Feature modules do not define flavor dimensions or touch keystores, adhering strictly to `AGENTS.md`.
- **Zero Circular Dependencies**: Modules strictly follow a directed acyclic graph (DAG): `app` → `feature` → `core`. Features NEVER depend directly on sibling features.
