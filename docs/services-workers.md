# Background Services & Workers Architecture

- **Purpose**: Reference for lifecycles, notification channels, foreground service types, and WorkManager jobs.
- **Audience**: Both (Human & Agent)
- **Related Source Paths**:
  - [`app/src/main/java/id/my/nanclouder/nanhistory/service/RecordService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordService.kt)
  - [`app/src/main/java/id/my/nanclouder/nanhistory/service/DataProcessService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/DataProcessService.kt)
  - [`app/src/main/java/id/my/nanclouder/nanhistory/service/RecordTileService.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordTileService.kt)
  - [`app/src/main/java/id/my/nanclouder/nanhistory/worker/AutoDeleteWorker.kt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/worker/AutoDeleteWorker.kt)
  - [`app/src/main/AndroidManifest.xml`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/AndroidManifest.xml)
- **Last Verified Against**: 2026-11-01

---

## 1. Services & Workers Matrix

| Component | Type | `foregroundServiceType` | Notification Channel ID | Primary Purpose |
|---|---|---|---|---|
| [`RecordService`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordService.kt) | Foreground Service | `location\|microphone` | `"Record"` / `"RecordDebug"` | Real-time audio recording, GPS track collection, sensor logging |
| [`DataProcessService`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/DataProcessService.kt) | Foreground Service | `dataSync` | `"DataProcess"` | Exporting backups, importing archives, rebuilding caches |
| [`RecordTileService`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/service/RecordTileService.kt) | Quick Settings Tile Service | N/A | N/A | Toggling `RecordService` state from the Android quick settings shade |
| [`AutoDeleteWorker`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/worker/AutoDeleteWorker.kt) | WorkManager `CoroutineWorker` | N/A | `"AutoDelete"` | Deferrable batch deletion of soft-deleted events older than 30 days |

---

## 2. Foreground Service Lifecycle & Channel Rules

```mermaid
sequenceDiagram
    participant App as UI / Tile / Intent
    participant Service as Foreground Service
    participant NM as NotificationManager
    participant System as Android OS

    App->>Service: startForegroundService(Intent)
    Note over Service: onCreate() / onStartCommand()
    Service->>NM: createNotificationChannel() (MUST exist prior to startForeground)
    Service->>Service: build Notification with ongoing flag
    Service->>System: startForeground(ID, notification, FOREGROUND_SERVICE_TYPE_*)
    Note over Service: Execute recording or data processing
    Service->>System: stopForeground(STOP_FOREGROUND_REMOVE)
    Service->>Service: stopSelf()
```

### Critical Service Rules
1. **Notification Channel Pre-Creation**: The notification channel (`Record`, `DataProcess`) MUST be registered with `NotificationManager` before `startForeground()` is invoked.
2. **Foreground Service Types**:
   - `RecordService` MUST declare and pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE`.
   - `DataProcessService` MUST declare and pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
3. **Android 12+ / 14 Restrictions**: Foreground services cannot be started while the app is in the background without an approved exemption.

---

## 3. WorkManager Job Specifications

- **Worker**: [`AutoDeleteWorker`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/src/main/java/id/my/nanclouder/nanhistory/worker/AutoDeleteWorker.kt)
- **Work Type**: `CoroutineWorker`
- **Execution Flow**:
  1. Queries database for soft-deleted events (`dao.getDeletedEvents()`).
  2. Compares `deletePermanently` timestamp against `Instant.now().toEpochMilli()`.
  3. Executes `safeDelete()` which removes associated audio and breadcrumb files.
  4. Optionally posts a completion notification to `"AutoDelete"` channel if enabled by user configuration.
- **Constraints**: Deferrable, non-critical, runs periodically under network/battery constraints.

---

## 4. Design Decisions & Fragile Areas

- **Fragile - Android 14 Background Service Start**: `RecordService` will fail with `ForegroundServiceStartNotAllowedException` if an attempt is made to start it from the background without a user-triggered foreground context (such as an Activity tap or an approved Quick Settings tile tap).
- **Fragile - 30-Day Permanent Deletion Rule**: `AutoDeleteWorker` enforces a strict 30-day grace period for soft-deleted items. Under no circumstances should non-expired items be purged.

---

## 5. Open Questions & Unverified Items
- None. Service declarations and worker logic are fully verified against `AndroidManifest.xml` and Kotlin sources.
