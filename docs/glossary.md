# Domain Glossary & Terminology

- **Purpose**: Reference for domain terms, core entity representations, data formats, and status flags.
- **Audience**: Both (Human & Agent)
- **Related Source Paths**:
  - [`app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt)
  - [`app/src/main/java/id/my/nanclouder/nanhistory/utils/history/`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/)
  - [`app/src/main/java/id/my/nanclouder/nanhistory/ui/main/EventSelectMode.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/ui/main/EventSelectMode.kt)
- **Last Verified Against**: 2026-11-01

---

## 1. Domain Entities & Models

| Domain Term | DB Entity | Domain Object | Definition |
|---|---|---|---|
| **Event** | [`EventEntity`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt#L45) | [`HistoryEvent`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/Events.kt) | A discrete logged occurrence or recording. Can be a single timestamp or time range, with optional audio, GPS tracks, and tags. |
| **Day** | [`DayEntity`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt#L78) | [`HistoryDay`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/Events.kt) | Aggregate grouping representing an entire calendar date (`LocalDate`), having daily descriptions and favorite flags. |
| **Tag** | [`TagEntity`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt#L86) | [`HistoryTag`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/Tags.kt) | Categorical label associated with events or days, rendered with a custom user-defined color tint. |
| **Location Breadcrumb** | [`LocationEntity`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/db/Tables.kt#L98) | [`LocationData`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/Events.kt) | Geographical point (latitude, longitude, speed, bearing, altitude, accuracy) tied to an event ID. |
| **Audio Recording** | N/A | File path string | Voice note or ambient sound recorded by `RecordService` stored in `context.filesDir/audio/`. |

---

## 2. Enumerations

### `EventTypes`
Defined in [`Enums.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/utils/history/Enums.kt):
- **`Point`**: Single point-in-time event with a start timestamp.
- **`Range`**: Spans an interval with `start` and `end` timestamps.

### `TransportationType`
Identifies travel mode for range-based journeys:
- `Unspecified`, `Walk`, `Bicycle`, `Motorcycle`, `Car`, `Train`, `Airplane`, `Ferry`.

### `EventSelectMode`
Controls query mode in list screens:
- `Default`: Standard historical chronological list.
- `Search`: Filtered by text query or tag IDs.
- `FavoriteEvent`: Only events flagged with `favorite = true`.
- `FavoriteDay`: Days flagged with `favorite = true`.
- `Deleted`: Soft-deleted events stored in trash.

---

## 3. Lifecycle States & Deletion Terms

- **Active**: Normal visible event (`deletePermanently IS NULL`).
- **Favorite**: Pinned or highlighted event/day (`favorite = true`).
- **Soft-Deleted / Trash**: Hidden event queued for permanent removal (`deletePermanently != NULL`). Retained for 30 days before eviction.
- **Permanent Purge**: Hard deletion of database rows and associated disk assets (audio and location files) by `AutoDeleteWorker`.

---

## 4. Open Questions & Unverified Items
- None. All domain terminology and enums are directly sourced from the codebase.
