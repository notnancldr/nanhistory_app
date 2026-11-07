# AGENTS.md - NanHistory

Rules, boundaries, and operational guidelines for AI agents working in this repository.

## 1. Project Overview & Scope
- **App Purpose**: Note events by adding them into the app, record events with location history, and record events with audio.
- **Module Structure**: Multi-module Gradle project:
  - App Shell: `:app` (Manifest, Application class, Flavors `plain`/`public`/`private`, Keystores, Composition Root `AppContainer`)
  - Feature Modules: `:feature:timeline`, `:feature:settings`, `:feature:eventdetail`, `:feature:record`, `:feature:map-location`, `:feature:yearlymap`, `:feature:sharemap`, `:feature:trash`, `:feature:tag`
  - Core Modules: `:core:common`, `:core:domain`, `:core:database`, `:core:data`, `:core:ui`
  - Root package: `id.my.nanclouder.nanhistory`.

## 2. Tech Stack & Verified Versions
- **Kotlin**: `2.4.0` (Compiler plugin `org.jetbrains.kotlin.plugin.compose` v`2.4.0`)
- **Android Gradle Plugin (AGP)**: `9.3.2`
- **Compose BOM**: `2026.05.01`
- **Material 3**: `1.4.0` (`androidx.compose.material3:material3-android`)
- **Room**: `2.7.1` (`androidx.room:room-runtime`, `androidx.room:room-compiler` via KSP)
- **WorkManager**: `2.9.0` (`androidx.work:work-runtime-ktx`)
- **OSMDroid**: `6.1.20` (`org.osmdroid:osmdroid-android`)
- **ColorPicker**: `1.1.2` (`com.github.skydoves:colorpicker-compose`)
- **SDK & Java**:
  - `minSdk`: `31`
  - `targetSdk`: `35`
  - `compileSdk`: `37`
  - JVM Target: `11` (`JavaVersion.VERSION_11`)
- **Annotation Processing**: KSP only (`com.google.devtools.ksp` v`2.3.6`). Kapt is NOT used.
- **Dependency Injection**: Manual / None. No Hilt or Koin.
- **Navigation**: Multi-Activity using standard Android `Intent`s. No Jetpack Navigation Compose.
- **Product Flavors**: `public` (generic encryption key), `private` (.env encryption key), `plain` (.env encryption key).
- **MapLibre**: Experimental only (`org.maplibre.gl:android-sdk:11.5.1`). OSMDroid is primary.

## 3. Documentation Index & Maintenance Rules
Detailed codebase architecture, subsystem behavior, and domain references reside in `docs/`. Read the relevant doc before modifying related code:
- [`docs/architecture.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/architecture.md): Read before making structural changes across activities, UI screens, ViewModels, or data layers.
- [`docs/packages.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/packages.md): Read to locate classes and verify package boundaries before adding new files or classes.
- [`docs/database.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/database.md): Read before touching Room entities, DAOs, migrations, or database queries.
- [`docs/services-workers.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/services-workers.md): Read before modifying background services, workers, audio/location recording, or notifications.
- [`docs/storage-saf.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/storage-saf.md): Read before working on file exports, imports, backups, SAF URIs, or internal caches.
- [`docs/maps.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/maps.md): Read before touching map rendering, GPS track visualization, or OSMDroid configurations.
- [`docs/build-release.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/build-release.md): Read before running builds, troubleshooting flavors, or checking encryption key requirements.
- [`docs/glossary.md`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/docs/glossary.md): Read to verify domain terminology, event statuses, and data modeling concepts.

**Maintenance Rule**: Agents MUST update the relevant `docs/*.md` file whenever they change, add, or refactor related code. Documentation must always reflect current verified code behavior, design decisions, and known fragile areas.

## 4. Module Layout & Architecture Dependency Rules
- **Directed Acyclic Graph (DAG)**: `:app` → `:feature:*` → `:core:*`.
- **Feature Isolation**: Features NEVER depend on sibling features or on `:app`.
- **Navigation**: Multi-Activity Intent navigation routed via `IntentRegistry` in `:core:common`.
- **Dependency Injection**: Manual DI via container interfaces in `:core:common.di` implemented by `AppContainer` in `:app`.
- **Core Separation**:
  - `:core:domain`: Pure Kotlin, zero Android dependencies. Entities, repository interfaces, use cases, math logic.
  - `:core:database`: Room entity definitions, DAOs, database migrations, Room schemas.
  - `:core:data`: Concrete repositories implementing domain interfaces using Room and local storage.
  - `:core:ui`: Shared Compose UI components, themes, base activities, dialogs, styling.
  - `:core:common`: Cross-module bridges, contracts, intent registry, DI interfaces, utilities.
- `ui` and `activity` MUST NOT call Room DAOs directly. All DB operations must go through repositories in `:core:data`/`:core:domain`.
- `debug` code MUST NEVER be referenced from release code paths.

## 5. State Management & ViewModel Rules
- Every screen reading from Room, a service, or settings MUST have a ViewModel.
- ViewModels live in `state`, one per screen or feature.
- ViewModels MUST expose an immutable `StateFlow` of UI state, collected in UI using `collectAsStateWithLifecycle()`.
- Composables below screen-level MUST receive state and lambdas, NOT ViewModels.
- `remember` and `mutableStateOf` are allowed ONLY for ephemeral UI state (dialog open, text field focus, animation progress, color picker draft).

## 6. Compose & UI Conventions
- **Naming**:
  - UI-emitting Composables: PascalCase nouns (e.g., `HistoryList`, `RecordButton`).
  - Value-returning Composables: camelCase.
  - Screen-level Composables end with `Screen` (e.g., `DayDetailScreen`). Exactly one screen composable per file, named after the file.
- **Modifiers**: Every UI-emitting composable MUST accept `modifier: Modifier = Modifier` as its first optional parameter and apply it to the root element.
- **Previews**:
  - Every new reusable UI composable MUST have at least one `@Preview`.
  - Provide both light and dark (`uiMode = UI_MODE_NIGHT_YES`) previews for components with color-dependent content.
  - Previews MUST NOT touch Room, services, or real settings. Use fake/sample data.
  - Preview functions MUST be `private` and named `<Component>Preview`.
  - Screen-level composables depending on ViewModels are exempt; preview their stateless inner content instead.
- **Theming**:
  - All UI MUST use `MaterialTheme.colorScheme`, `typography`, and `shapes`.
  - Hardcoded colors (`Color(0xFF...)`) are FORBIDDEN outside theme definitions and the ColorPicker feature.
  - Dynamic color is DISABLED.
  - Dark mode: user override in settings.
- **Animation**:
  - Use Compose Animation APIs (`animate*AsState`, `AnimatedVisibility`, `updateTransition`). View-based animators are FORBIDDEN.
  - Animations MUST be short, purposeful, and MUST NOT cause item recomposition across long lists.
  - MUST respect system "remove animations" setting.
- **ColorPicker**: Allowed ONLY in tag color selection. Forbidden everywhere else without prior user approval.

## 7. Room Database & Migrations
- Manual migrations are REQUIRED for every database version bump.
- `fallbackToDestructiveMigration()` is STRICTLY FORBIDDEN.
- Schema export MUST remain enabled (`exportSchema = true`), and exported schema JSON files in git must be preserved.
- Every migration MUST have a migration test using `MigrationTestHelper`.
- **Naming**:
  - Entities: singular PascalCase with `Entity` suffix (e.g., `EventEntity`).
  - DAOs: `<Entity>Dao` (e.g., `AppDao`).
  - Table names: `snake_case`.
- Threading: DB calls must execute off the main thread (`Dispatchers.IO`).

## 8. Storage & Scoped Storage (SAF)
- User-visible exports and imports MUST use SAF (`DocumentFile`, `ACTION_OPEN_DOCUMENT_TREE`, `ACTION_CREATE_DOCUMENT`).
- Internal app data (DB files, caches, temporary files) MUST use `context.filesDir` or `context.cacheDir`.
- Persist every user-granted tree or document URI using `takePersistableUriPermission` and store the URI string.
- Raw `java.io.File` paths to external storage and `requestLegacyExternalStorage` are STRICTLY FORBIDDEN.
- Check that a persisted URI is still valid before use and handle revoked permissions gracefully.

## 9. Maps (OSMDroid) Rules
- MUST set a unique user-agent (the app ID `id.my.nanclouder.nanhistory`) in `Configuration.getInstance()` before the first `MapView` is created.
- Point OSMDroid base path and tile cache to app-specific storage with a size cap.
- Wire `MapView.onResume()`, `onPause()`, and `onDetach()` to lifecycle using `AndroidView` with `DisposableEffect` and `LifecycleEventObserver` in Compose.
- Bulk-downloading tiles from public OSM servers is FORBIDDEN.
- Offline behavior: show cached tiles and NEVER crash on network failure.
- MapLibre is an experimental feature.

## 10. Background Work & Services
- **Foreground Service**: Used for user-initiated, ongoing, and time-critical/visible work (`RecordService` for audio recording, `DataProcessService` for processing).
  - MUST declare correct `foregroundServiceType` in manifest and pass it in `startForeground()`.
  - MUST create a dedicated notification channel before `startForeground()` with a persistent ongoing notification.
  - NEVER start a foreground service from the background on Android 12+ without an allowed exemption.
- **WorkManager**: Used for deferrable, guaranteed, constraint-based work (`AutoDeleteWorker`).
  - Workers MUST be idempotent, use unique work names, and set constraints explicitly.
- Custom battery-optimization exemption requests and exact alarms for periodic work are FORBIDDEN.

## 11. Concurrency & Coroutines
- `Dispatchers.IO`: Room (if not main-safe), file/SAF operations, network.
- `Dispatchers.Default`: CPU-heavy audio/sensor processing.
- `Dispatchers.Main`: UI only. NEVER block it.
- **Allowed Scopes**:
  - ViewModels: `viewModelScope`
  - Activities: `lifecycleScope` or `repeatOnLifecycle`
  - Composables: `rememberCoroutineScope`
  - Services: service-owned `CoroutineScope(SupervisorJob() + dispatcher)` cancelled in `onDestroy()`
  - Workers: `CoroutineWorker`'s own scope
- `GlobalScope` is STRICTLY FORBIDDEN.
- NEVER hardcode dispatchers inside classes; inject them or accept them as parameters.
- `runBlocking` is FORBIDDEN outside tests.

## 12. Code Style & Documentation
- **Formatter**: ktlint standard. Run on every touched file; NEVER reformat untouched code.
- **Line Length Limit**: 120 characters.
- **File Size Limit**: 400 lines; split beyond that.
- **KDoc**: REQUIRED on public classes, services, workers, DAOs, and non-obvious functions.
- **Comments**: MUST explain *why*, not *what*. Commented-out code is FORBIDDEN.
- **Language**: English for all code identifiers, comments, and KDoc.

## 13. Localization & String Resources
- Hardcoded user-facing strings in Compose or Kotlin are STRICTLY FORBIDDEN. Use `stringResource(R.string.*)` or `context.getString()`.
- All user-facing strings live in `res/values/strings.xml`.
- Supported & default locale: `en`.
- New strings MUST be added to all supported locales in the same change.
- Use `pluralStringResource` for counts and format arguments over string concatenation.
- Notification text and service strings MUST follow these rules.
- Strings used only in `debug` code are exempt.

## 14. Testing Rules
- Frameworks: JUnit4, MockK, `kotlinx-coroutines-test` (`runTest`, injectable dispatchers).
- Room migrations: `MigrationTestHelper` for every migration.
- Compose UI tests: REQUIRED for new screens.
- Every bug fix MUST include a regression test where practical (or state why).
- New logic in `state`, `db`, and `utils` MUST have unit tests.
- UI-only changes need no unit tests but MUST have previews.
- New test dependencies require prior approval.

## 15. Build & Verification Commands
Before marking any task done, all of the following commands MUST pass with zero errors (using the `plain` flavor by default):
1. `./gradlew assemblePlainDebug`
2. `./gradlew testPlainDebugUnitTest`
3. `./gradlew lintVitalPlainRelease` (matches `lint-baseline.xml`; if using `lintPlainDebug`, note that pre-existing baseline mismatch warnings may appear

If any command fails for an unrelated reason, report it. Do not fix unrelated failures or suppress warnings to pass.

## 16. Permissions & Privacy
- Adding, removing, or changing any `<uses-permission>` or `foregroundServiceType` requires explicit prior user approval.
- Request runtime permissions just-in-time when the feature is used, NEVER at app launch.
- Handle runtime requests in UI (Activity/Composable via Activity Result API), NEVER inside a Service or Worker. Services must check permissions and fail gracefully.
- Handle "denied" and "denied permanently" states with a path to app settings.
- Location, audio, and history data are strictly private: NEVER log them, transmit them over network, or include them in crash reports.
- Analytics/telemetry/tracking libraries are FORBIDDEN.

## 17. Dependency Management Policy
- Agents MUST NOT add, remove, or upgrade any dependency or plugin without explicit prior approval.
- Changes to AGP, Kotlin, Compose BOM, `compileSdk`, and `targetSdk` always require approval. Security patch upgrades also require approval.

## 18. Git Rules
- Branch naming: `feat/<short-name>`, `fix/<short-name>`, `chore/<short-name>`.
- Commit format: Conventional Commits `type(scope): subject` in English.
- Agents MUST NOT commit, push, merge, rebase, or force-push unless explicitly requested by the user. Default is to leave changes uncommitted.
- Exactly one logical change per commit.

## 19. Forbidden Actions
- NEVER modify or touch keystores, signing configs, `keystore.properties`, `local.properties`, or `.env*`.
- NEVER modify existing exported Room schema JSON files under `app/schemas/**` or shipped migrations.
- NEVER use `fallbackToDestructiveMigration()`.
- NEVER use `GlobalScope` or `runBlocking` (outside tests).
- NEVER use raw `java.io.File` paths to external storage or `requestLegacyExternalStorage`.
- NEVER bulk-download OSM tiles from public servers.
- NEVER hardcode user-facing strings or colors in UI code.
- NEVER add or upgrade dependencies without explicit user approval.
- NEVER commit, push, or alter git branches without explicit user request.

## 20. Ask-First Actions & Fragile Areas
**Ask-First Actions**:
- Any database schema change or version bump.
- Any change to `<uses-permission>`, foreground service types, or manifest components.
- Adding, removing, or upgrading dependencies.
- Changes to `RecordService`, `DataProcessService`, or `AutoDeleteWorker` behavior.
- Deleting files, renaming packages, or moving code across packages.
- Anything affecting how user data is deleted or exported.

**Known Fragile Areas**:
- `RecordService`: Breaks if started from the background on Android 14+ without valid exemption.
- `AutoDeleteWorker`: MUST NEVER delete data newer than 30 days.

## 21. When Uncertain: STOP and Ask
When uncertain about any rule, architecture decision, or repository behavior, STOP and ask the user. Never guess or assume.
