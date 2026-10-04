# ExamVerse Android

The active application is this normal Android Gradle project. See the repository [README](../README.md) for v5 features, installation, permission behavior and verification instructions.

Build with JDK 17, Gradle 8.11.1 and Android SDK Platform 36:

```bash
gradle :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Core exams and study data remain offline. Health Connect and Usage Access are optional. Alarm exact scheduling and full-screen access are explained in Alarm Studio. The APK published in GitHub Releases is a personal/debug build.
