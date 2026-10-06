# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MITSO (university) schedule client for Android. Three Gradle modules (`settings.gradle.kts`), UI text and commit messages are in Russian:

- `:core` — shared Android library (`mitsoschedule.core`): models, network clients, schedule normalization. No UI.
- `:app` — phone app (`mitsoschedule.app`), Jetpack Compose + Material 3, minSdk 31.
- `:watchso` — Wear OS companion (`by.iposdev.watchso`), Wear Compose Material 3, Tiles and Complications, minSdk 34.

Both use JVM toolchain 17, compileSdk/targetSdk 37, and version catalog `gradle/libs.versions.toml`.

## Commands

```
./gradlew :app:assembleDebug            # phone debug APK
./gradlew :watchso:assembleDebug        # watch debug APK
./gradlew :app:assembleRelease          # release (signed only if keystore props present)
./gradlew :core:testDebugUnitTest       # unit tests (shared logic: normalizer, dates/weeks, repository, store, cipher)
./gradlew :app:testDebugUnitTest        # unit tests (app)
./gradlew :watchso:testDebugUnitTest --tests "by.iposdev.watchso.MainViewModelTest"   # single test class
```

Release signing for `:app` comes from `KEYSTORE_BASE64` or `KEYSTORE_FILE`, plus `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (Gradle property or env var); without a keystore the release build is unsigned. Both release builds are minified with R8 (`:app` and `:watchso`; `:watchso` is signed with the debug key). Keep rules for the shared models live in `core/consumer-rules.pro`, so they apply to both apps. CI is `.github/workflows` (builds on pushes to canary/nightly/beta/release/main; it parses the `CHANNEL` value out of `app/build.gradle.kts`, so keep that `channelOverride` block's shape).

## Architecture

- **Backend:** the apps do not scrape the MITSO site. They call a custom server (`PreferencesManager.DEFAULT_SERVER_URL`, user-overridable in settings) under `/api/v1/schedule/{faculties,forms,courses,groups,weeks,...}` plus `/health`. The selection is a cascade: faculty → form → course → group → week(s).
- **Shared layer (`:core`):** no UI, everything both apps need.
  - *Network:* `WebWorker` (schedule API) and `StudentWebWorker` (cabinet; `parseCabinetHtml` is regex-based) take an injectable `OkHttpClient` (`ServerConfig.newHttpClient`); the watch passes one with IPv4-first DNS and a TLS trust manager tolerant of wrong system clocks (`watchso/.../data/WatchNetwork.kt`). `WebWorker` returns `ApiResult` (`Failure(ApiError(kind NETWORK/HTTP/PARSE))`) so UI can tell "server unreachable" from "schedule not published"; `StudentWebWorker` returns `kotlin.Result`.
  - *Logic:* `ScheduleRepository` (options cascade, refresh all weeks, load one week, caching), `Weeks` (week list/navigation/current-week resolution), `ScheduleDates` (date parsing, staleness, auto-refresh policy), `TodayStatus` (where we are in today's timetable; returns data only), `ScheduleNormalizer` (extracts room/type/teacher/subgroups from raw lesson text, merges same-time lessons), `StudentSession`.
  - *Storage:* `ScheduleStore` wraps DataStore (phone keys have no prefix, watch keys use `watch_`). Student login/password and the cabinet cache (holds the Moodle password) are AES-GCM encrypted with an Android Keystore key (`SecretCipher`, `enc1:` prefix); old plaintext is migrated by `migrateLegacySecrets()`, and an undecryptable value counts as "not logged in".
  - *Strings:* `UiStrings`/`reason(ApiError)` give ViewModels resource strings without a Context; the network-failure reasons live in `core/src/main/res`.
  - *Test helpers in main sources:* `core/testing/FakeServer` (OkHttp interceptor with canned JSON, can go offline) and `InMemoryDataStore`. File-based DataStore fails on repeated writes in JVM tests on Windows, so tests use the in-memory one.
- **State and DI:** each app has one `MainViewModel` (plain `ViewModel`) with Compose `mutableStateOf` exposed as `State<T>` (not Flow). Dependencies come through the constructor; `AppContainer` (phone) and `WatchContainer` (watch) build them and provide the `viewModelFactory`. Only screen-specific state lives in the ViewModels (watch: screens, picker step; phone: server URL, theme, health). `Weeks.ALL_ID = "ALL"` is the sentinel for "all weeks".
- **Navigation:** no nav library; `AppTab` (phone) and `WearScreen` (watch) enums drive screens; enum order decides the transition direction. Bottom bar is `ExpressiveNavBar`.
- **Phone schedule tab:** list state is hoisted to `MainAppScreen` (scroll survives tab switches); `PullToRefreshBox` wraps the schedule and cabinet lists; a "↑/↓ Сегодня" chip appears when today's day is off-screen (hidden at the very top so it does not cover the group header), and the list auto-scrolls to today if it is entirely off-screen. `todayIndex` assumes the item order header → past-days accordion (+ its days) → today, keep it in sync when reordering `LazyColumn` items.
- **Watch schedule header:** `WearScheduleHeader` merges group + week navigation into one card (tap centre = group picker; refresh is pull-down or the bottom chip).
- **Theming:** `MitsoTestTheme(darkTheme, dynamicColor)`. With dynamic colors on (setting `AppSettings.dynamicColorFlow`) the `ColorScheme` comes from `dynamicDark/LightColorScheme`; Biolume shapes, relief and typography stay, and `BiolumeDepthTokens.recoloredFor(scheme)` recolors the signal layer (glow, selection fill). Card shapes live in `ui/theme/BiolumeShapes` (28dp cards; `connected(index, count)` for rows of related cards, used for a day's lessons) and `ui/components/BiolumeDivider` (Fade inside cards, Node between days). Rules are in `Biolume-Design-Guidelines.md` §4.3.
- **Home-screen widget (phone):** `widget/ScheduleWidget` (Glance, responsive 2x2 / wide / large, Material You colors) renders `buildWidgetState(selection, cachedSchedule, now)` (pure, unit-tested; reuses `TodayStatus` and `ScheduleNormalizer`). Updated when the app refreshes (`MainViewModel.onScheduleUpdated` via `AppContainer`), by its own refresh button (`RefreshScheduleAction` -> `AppContainer.refreshSchedule()`) and the 30-minute system period; it does not tick at lesson boundaries. Settings has an "add to home screen" button (`requestPinScheduleWidget`). Reinstalling over adb removes placed widgets on the emulator launcher; add it again.
- **Watch surfaces:** besides the activity, `tile/ScheduleTileService` and `complication/MainComplicationService` render schedule data (they build their own `WebWorker` via `createWatchWebWorker()`).
- **Strings:** UI text is in `strings.xml` of each module (Russian only). Left as code on purpose: lesson-type/month keyword matching, sample data in previews, `Weeks.ALL_OPTION`/`UserSelection` default labels in `:core` models, and the lesson-type labels in `WearLessonCard`.
- **Large UI files** are split by component (`ui/components/` on the phone, `presentation/components/` on the watch).

## Testing

JVM unit tests only; ViewModel tests use `FakeServer` + `InMemoryDataStore` + `Dispatchers.setMain(UnconfinedTestDispatcher())` and poll with a timeout because requests run on `Dispatchers.IO`. `testOptions.unitTests.isReturnDefaultValues` is on in all modules because `:core` uses `android.util.Log`. Nothing exercises real Keystore or R8 output; check those on a device.

## Design system

UI follows "Biolume" (`Biolume-Design-Guidelines.md`, v2.1): Material 3 Expressive roles/shapes plus neumorphic structural depth, with colored glow reserved for a single focus/selection/live signal at a time. Themes are Abyss (dark) and Tidepool (light). Tokens are implemented in `app/.../ui/theme/` (`Biolume*.kt`, `Color.kt`, `Type.kt`) and reused in `ui/components/`; consult the guideline doc before changing colors, shadows, or motion.
