# FitCoach

FitCoach is a native Android personal training and sports nutrition coach. It is designed offline-first: local profile, plan, workout, progress, and notification data remain usable without network access. Conversational AI is an optional provider-backed layer and does not replace deterministic calculations.

## Technology

Kotlin, Jetpack Compose, Material 3, MVVM/Clean Architecture, Room, Coroutines/Flow, Hilt, WorkManager/AlarmManager, DataStore, Retrofit/OkHttp, and modular Gradle builds.

Minimum Android version: API 26.

## Build

```bash
/workspace/build.sh :app:assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Delivery roadmap

1. Foundation: clean multi-module project, dark Compose shell, Room profile persistence, onboarding, navigation.
2. Nutrition: verified food database, energy formulas, macro consistency, carb cycling, seven-day plans, substitutions.
3. Training: exercise database, periodized plans, session logging, double progression, fatigue safeguards.
4. Adaptive coaching: weekly check-ins, deterministic analysis, explainable proposals, approval and rollback.
5. AI: provider abstraction, secure credential handling, structured context, controlled tools, error handling.
6. Notifications and insights: scheduled reminders, charts, export, backup and restore.
7. QA: unit, Room, algorithm, offline, UI, notification and device validation, then signed release packaging.

The current branch contains the working foundation. Features are added in small verified slices; no screen is treated as complete merely because it is navigable.
