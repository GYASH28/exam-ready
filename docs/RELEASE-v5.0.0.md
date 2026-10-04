# ExamVerse 5.0.0 — Anime Study OS

This release fixes the alarm that only began ringing after a notification click, rebuilds all four widgets for compact sizes and adds six illustrated study worlds plus a new Study Lab.

## Download

Install **ExamVerse-v5.0.0.apk** on Android 8.0 or later. The attached `.sha256` file verifies the download.

## What changed

- Naruto, Dragon Ball, Bleach, Black Clover, Demon Slayer and One Piece visual themes, with bundled illustrated scenes, native component/dialog accents, ambient motifs and matching widget art. Five themes have character scenes; Dragon Ball uses an original cosmic training environment.
- Background alarm playback through a foreground audio service; notification snooze/dismiss, persistent ten-minute snooze, sound fallback and permission diagnostics with a twenty-second test.
- Four widgets resize down to 40dp layouts and adapt their text/details to the available space. Actual 1×1 availability depends on the launcher.
- Persisted focus sessions, background completion notifications, custom durations and recovery breaks.
- Spaced-repetition flashcards, search and a mistake notebook that converts corrections into recall cards.
- Manual revision missions, task-linked focus, weak-topic planning, overdue visibility and editable topic confidence.
- Study backup/export and validated restore.
- Fixes for duplicate focus logging, repeated XP rewards, stale streaks, system bars, native dialog readability and Android API compatibility.

## Verification

The APK and test APK compile successfully; Android lint passes with no errors. All **8 Android 15 emulator regression tests** pass, including service audio playback while locked with full-screen access denied and no notification click. APK v2 signature verification passes.

The emulator validates playback state; physical phone speaker volume and manufacturer battery restrictions have not been tested.

## After installation

Open **Studio → Alarms** and enable Alarms & reminders access. The readiness panel also checks notifications and lock-screen/full-screen access. Use the twenty-second test with your screen locked. Alarm volume follows the phone's alarm-volume setting.

This is a personal/debug build. Android only upgrades APKs with matching signing certificates; previous GitHub Actions builds may use a different debug key. If Android reports a signature conflict, preserve your existing study data before considering removal of the older app.

Core study data remains offline. Health/Usage Access are optional. Generated franchise fan art is for this personal build and is not official licensed artwork.
