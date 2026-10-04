# ExamVerse v5 validation

Verified on 4 October 2026 against the normal `android/` source tree.

## Compilation and lint

- JDK 17, Gradle 8.11.1, Android SDK 36.
- `:app:assembleDebug` — passed.
- `:app:assembleDebugAndroidTest` — passed.
- `:app:lintDebug` — passed with no errors. Existing style/deprecation warnings remain; the report is generated under `app/build/reports/`.
- Android manifest and packaged APK identify `com.brace.examverse`, version code 5, version 5.0.0, minimum SDK 26 and target SDK 35.
- APK Signature Scheme v2 verification — passed for the personal/debug signing certificate.

## Android 15 emulator regression suite

`UpgradeInstrumentedTest`: **8 tests passed**.

1. All six theme images decode and render.
2. Dark native dialogs have a readable dark background.
3. Flashcard intervals and mistake-resolution persistence.
4. Study backup round trip and rejection of invalid progress values.
5. Focus-session recovery, single completion logging and no credit for breaks.
6. Planner deadlines, overdue visibility and single-award task XP.
7. Automatic alarm playback while locked, with no notification tap, plus exact ten-minute snooze and cancellation.
8. All four RemoteViews layouts apply and fit at 40×40dp without missing-view failures.

For the alarm test, exact-alarm access and notifications were enabled, while **full-screen intent access was explicitly denied**. The assertion checked the foreground service's actual `MediaPlayer.isPlaying()` state. The emulator's host audio output was disabled, so this does not establish loudness on a physical speaker.

## Visual checks

The dashboard, Black Clover focus scene, Study Lab and native recall dialog were reviewed on a 360dp-wide emulator viewport. Screenshots under `docs/screenshots/` use synthetic demonstration study data rather than a user's records.

## Limits

- No physical phone/OEM battery-management testing was performed.
- The launcher decides its smallest supported cell size; 40dp app layout support cannot override launcher grid restrictions.
- Health Connect requires a compatible provider and granted permissions; physical health data was not exercised in these regressions.
- The release is a signed personal/debug APK. An older APK signed by another key cannot be upgraded in place. Preserve data before considering removal of an older installation.
- GitHub Actions independently compiles the APK and test APK and runs lint; it does not execute the emulator suite in the current workflow.
