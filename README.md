# ExamVerse v2 — Anime Study OS

ExamVerse is an original, offline-first Android exam countdown and study planner inspired by the public feature category of countdown/study-planner apps. It does **not** copy LazyByte source code, private implementation, screenshots, or bundled assets.

## What changed in v2

This is a major rebuild of the original MVP rather than a visual refresh.

### Mission Control
- Live countdown to the next exam
- Readiness score per exam
- Priority, difficulty and target-score profile
- Today’s focused minutes vs daily goal
- Study streak and pending mission count
- Today’s generated revision missions
- Smart-plan quick action

### Smart Revision Planner
- Syllabus topics belong to specific exams
- Each topic tracks difficulty, confidence and estimated study time
- One-tap automatic revision-plan generation
- Revision missions are weighted by exam priority and topic difficulty
- Hard topics automatically receive extra revision passes
- Daily mission queue with completion XP
- Per-exam syllabus map and readiness tracking

### Focus Dojo
- 25/5 Pomodoro, 50/10 Long Focus and 90/20 Deep Work modes
- Attach a focus session to an exam and syllabus topic
- Pause or finish early and still log productive minutes
- Daily goal progress
- Real study-session history
- Topic confidence improves when it is actively reviewed

### Study Intelligence
- XP + levels
- Anime-theme rank progression
- Seven-day focused-minutes chart
- Total focus time, session count and streak
- Readiness view across upcoming exams
- Study-time breakdown by subject

### Exam profiles
- Categories, notes, date and time
- Priority: 1–5
- Difficulty: 1–5
- Target score: 1–100%
- Configurable reminder: 6h / 12h / 24h / 48h / 72h
- Edit, complete, reopen and delete flows

### Theme systems
Three full fan-style visual modes using original abstract shapes and palettes:
- **Shinobi Mode** — Naruto-inspired orange, ink and paper language
- **Saiyan Mode** — Dragon Ball-inspired gold, cobalt and energy language
- **Soul Reaper Mode** — Bleach-inspired monochrome, crimson and blade language

Themes change backgrounds, accents, progression rank names, motivational copy and widgets. No copyrighted character art, logos or screenshots are bundled.

### Four Android home-screen widgets
- Next Exam
- Live Countdown
- Upcoming Exams
- **Daily Mission** — focused minutes, daily goal, mission count, streak and level

## Offline-first

No account, backend, ads, analytics SDK or internet connection is required. App data is stored locally in Android SharedPreferences as structured JSON.

## Build an installable APK

The repo includes `.github/workflows/build-apk.yml`. Push the project to a GitHub repository, open **Actions → Build ExamVerse v2 APK → Run workflow**, then download the `ExamVerse-v2-installable-apk` artifact. It contains `ExamVerse-v2.0.0.apk` plus a SHA-256 checksum.

### Android Studio
1. Open this folder in Android Studio.
2. Use JDK 17.
3. Install Android SDK Platform 35 and Build Tools 35.0.1.
4. Sync Gradle.
5. Run `assembleDebug` or choose **Build → Build APK(s)**.
6. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Package

`com.brace.examverse`

## Publishing note

For public Play Store distribution, use properly licensed franchise branding/artwork or rename the fan-style themes to fully original names. The current source intentionally avoids bundled copyrighted anime artwork.
