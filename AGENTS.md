# FitCoach repository guide

## Current state

FitCoach 2.0 is a clean native Android rebuild. The previous application was archived outside the working tree before the reset. This branch intentionally contains no imports from the archived implementation.

## Build

Use `/workspace/build.sh :app:assembleDebug` from the repository root. The wrapper uses the repository-local Android SDK and JDK 17. Run `/workspace/build.sh test` for JVM/unit tests and `/workspace/build.sh connectedDebugAndroidTest` only when an Android device or emulator is available.

## Architecture

- `app`: Android application, Compose navigation, screens, dependency injection.
- `domain`: platform-light models and use-case contracts.
- `database`: Room entities, DAOs, migrations, and database configuration.
- `data`: repository implementations and local persistence adapters.
- `nutrition`, `workout`, `calendar`, `progress`, `coaching`, `notifications`, `ai`: feature modules with deterministic engines and infrastructure adapters.
- UI state is exposed by ViewModels using Kotlin Flow. Mathematical decisions remain deterministic and are separate from conversational AI.

## Product rules

Do not fabricate user measurements or food values. Keep estimated values distinct from recorded values. Important plan changes must be represented as reviewable proposals before application. Provider/API keys must never be committed or bundled in the APK. Safety limits must reject extreme nutrition targets, unsafe progressions, pain-related progression, and medical claims.

## Working rules

Keep changes focused. Add tests for deterministic calculations and persistence behavior. Build after each feature slice. Do not mark features complete unless the implementation and verification exist.
