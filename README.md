# ExamVerse v4 — Exam Readiness OS

ExamVerse is an original, offline-first Android exam countdown, revision planner, focus tracker and readiness dashboard. v4 turns the earlier utility-style app into a much more visual study OS with premium theme systems, richer analytics, better widgets and a clearer daily workflow.

The project does **not** copy LazyByte source code, private implementation, screenshots or bundled assets.

## v4 highlights

### Mission Control
- Live next-exam countdown with date, priority, target score and readiness
- Daily focus goal, streak and average readiness at a glance
- "Today's Command" card that points to the most useful next action
- One-tap Add Exam and Smart Plan actions
- Upcoming exam cards with deadline and readiness context
- New readiness map directly on the dashboard

### Study Intelligence
ExamVerse now includes custom lightweight charts rather than a heavy chart dependency:

- **7-day Focus Trend** — line + area visualization with an average line
- **28-day Consistency Dot Graph** — dot size represents focused minutes
- **Pressure vs Readiness Dot Graph**
  - X axis = time until exam
  - Y axis = readiness
  - Dot size = exam priority
  - Dot state reflects readiness risk
- Per-exam readiness progress
- Subject study-time balance
- 7-day average focus
- 28-day active-day count
- "Needs attention" exam signal

### Smart Revision Planner
- Syllabus topics belong to individual exams
- Topic difficulty, confidence and estimated study time
- Automatic revision-plan generation
- Harder and higher-priority material receives more planning weight
- Daily mission queue and XP rewards
- Per-exam readiness tracking

### Focus Engine
- 25/5 Pomodoro
- 50/10 Long Focus
- 90/20 Deep Work
- Attach sessions to an exam and syllabus topic
- Pause, reset or finish early and log productive time
- Daily-goal progress and streak context
- Real study-session history

### Balance OS
Optional on-device context lives beside study data without becoming a requirement.

**Android Usage Access**
- Today's screen time
- Unlock count
- Top-used apps
- App usage progress bars

**Health Connect**
- Steps
- Sleep duration
- Average heart rate
- Calories burned
- Exercise duration

Both are opt-in. The planner, countdowns, analytics and focus system work without these permissions.

### Premium visual themes

Three full fan-style visual systems use original abstract graphics rather than copyrighted character art:

- **Shinobi Ember** — warm parchment, ember orange, burgundy ink, seal/spiral motion
- **Saiyan Energy** — deep cosmic blue, gold energy, cobalt beams and animated aura
- **Soul Reaper Noir** — ink black, crimson slash geometry, blade-light accents

Themes now change:
- full color system
- glass surfaces
- hero gradients
- animated ambient backdrop
- rank progression
- motivational copy
- widget styling

The animation is intentionally subtle and lightweight so the app still behaves like a productivity tool.

### Widget Studio

Four Android home-screen widgets:
- Next Exam
- Live Countdown
- Upcoming Exams
- Daily Mission

Each installed widget can be customized independently:
- theme
- glass opacity
- theme glow strength
- corner softness
- compact/detailed density
- selected exam for live countdown

Widgets also surface readiness information and use layered glass rendering instead of a flat background.

### Alarm Studio
- Exact custom alarms
- Once, daily and weekday repeat rules
- Snooze
- Custom sound support
- Re-scheduling after device restart
- Exam reminders

## Offline-first and privacy

Core app data is stored locally on-device. The build requires no account, backend, ads or analytics SDK.

Health Connect and Usage Access are optional Android permissions used only for the Balance OS features.

## Source layout

The normal Android Studio project now lives in:

`android/`

Important areas:

```text
android/app/src/main/java/com/brace/examverse/
├── MainActivity.java
├── data/
├── alarms/
├── theme/
├── visuals/
├── wellness/
└── widgets/
```

## Build an installable APK

### GitHub Actions

The repository includes a workflow that compiles the normal `android/` source tree and uploads:

`ExamVerse-v4.0.0.apk`

with a SHA-256 checksum.

### Android Studio

1. Open the `android/` folder in Android Studio.
2. Use JDK 17.
3. Install Android SDK Platform 36 and Build Tools 35.0.1.
4. Sync Gradle.
5. Run `assembleDebug` or choose **Build → Build APK(s)**.
6. Debug APK output:
   `android/app/build/outputs/apk/debug/app-debug.apk`

## Android package

`com.brace.examverse`

- minSdk: 26
- targetSdk: 35
- compileSdk: 36
- version: 4.0.0

## Publishing note

The current private-build theme names intentionally reference familiar anime inspiration, but the app does not bundle copied character artwork, screenshots or logos. For public store distribution, use properly licensed franchise branding or rename the themes to fully original branding.
