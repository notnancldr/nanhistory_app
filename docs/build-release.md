# Build & Release Configuration

- **Purpose**: Reference for Gradle configuration, product flavors, encryption keys, version code management, and verification commands.
- **Audience**: Both (Human & Agent)
- **Related Source Paths**:
  - [`build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/build.gradle.kts)
  - [`settings.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/settings.gradle.kts)
  - [`gradle/libs.versions.toml`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/gradle/libs.versions.toml)
  - [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts)
  - [`.env.example`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/.env.example)
  - [`versionCode.txt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/versionCode.txt)
  - [`increment.txt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/increment.txt)
- **Last Verified Against**: 2026-11-01

---

## 1. Build Environment & SDK Levels

| Setting | Value | Defined In |
|---|---|---|
| Gradle Module | `:app` | [`settings.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/settings.gradle.kts) |
| AGP Version | `9.3.2` | [`gradle/libs.versions.toml`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/gradle/libs.versions.toml) |
| Kotlin Version | `2.4.0` | [`gradle/libs.versions.toml`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/gradle/libs.versions.toml) |
| KSP Version | `2.3.6` | [`build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/build.gradle.kts) |
| `compileSdk` | `37` | [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts) |
| `targetSdk` | `35` | [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts) |
| `minSdk` | `31` | [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts) |
| JVM Target | `11` | [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts) |

---

## 2. Product Flavors & Build Types

The app defines dimension `version` with three distinct flavors:

| Flavor | Application ID Suffix | Version Name Suffix | Encryption Keys Source |
|---|---|---|---|
| `public` | `.pub` | `-pub` | Uses generic hardcoded default keys defined in `build.gradle.kts` |
| `private` | `.private` | `-pvt` | Loaded from `.env` (`ENCRYPTION_KEY`, `LEGACY_ENCRYPTION_KEY`) |
| `plain` | *(none)* | *(none)* | Loaded from `.env` (`ENCRYPTION_KEY`, `LEGACY_ENCRYPTION_KEY`) |

### Build Types
- **`debug`**: `applicationIdSuffix = ".dev"`, `versionNameSuffix = "-d"`.
- **`release`**: `versionNameSuffix = "-r"`, `isMinifyEnabled = false`, Proguard rules from `proguard-rules.pro`.

---

## 3. Dynamic Version Code Management

In [`app/build.gradle.kts`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/app/build.gradle.kts#L24-L34):
- `versionCode` is tracked in [`versionCode.txt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/versionCode.txt).
- An auxiliary file [`increment.txt`](file:///home/nandaahmad/Projects/AndroidStudio/NanHistory/increment.txt) controls auto-incrementing:
  - If `increment.txt` exists and is NOT blank, `versionCode` increases by 1.
  - After evaluating, `increment.txt` is wiped to an empty string.
- Agents MUST NOT manually tamper with `versionCode.txt` unless instructed.

---

## 4. Verification Commands

Before marking any task complete, all three commands MUST pass cleanly (using the `plain` flavor by default):

```bash
# 1. Build and compile debug APK
./gradlew assemblePlainDebug

# 2. Run local unit tests
./gradlew testPlainDebugUnitTest

# 3. Execute Android Lint analysis against baseline
./gradlew lintVitalPlainRelease
```

> [!NOTE]
> `lint-baseline.xml` was generated against `lintVitalRelease`. Running `lintPlainDebug` evaluates debug-specific lint checks not covered by the baseline, which may flag pre-existing baseline mismatches in legacy files.

### Environment Requirements
- **JDK with `javac`**: Set `JAVA_HOME=/home/nandaahmad/dev/android-studio/jbr` (the system OpenJDK 25 headless package lacks `javac`).
- **Android SDK Path**: SDK resides at `/home/nandaahmad/dev/Android/Sdk` as configured in `local.properties`. Gradle executions in sandbox environments require sandbox bypass (`BypassSandbox: true`) to access paths outside the workspace.
- **Writable Android User Home**: In constrained shell environments where `~/.android` is read-only, export `ANDROID_USER_HOME=/tmp/.android`.

---

## 5. Open Questions & Unverified Items
- None. Build configurations and verification tasks are verified directly against root and module build files.
