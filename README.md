# Halter

Halter is a Kotlin + Jetpack Compose Android app for local-first digital wellbeing.

It is built around app blocking, short-video scroll limits, and a breathing pause before entering user-flagged distracting apps. The project intentionally has no network permission, no account system, no Firebase, no analytics SDK, and no proprietary ad stack.

## Current Build

- Package: `com.ujwal.halter`
- Minimum SDK: 26
- Target SDK: 36
- UI: Jetpack Compose + Material 3
- Storage: Room + DataStore
- Background work: WorkManager
- Dependency injection: Koin
- License: GPL-3.0-or-later

## Implemented Foundations

- Onboarding for Accessibility, Usage Access, and Draw Over Apps permissions.
- Installed app list and per-app monitoring settings.
- Room entities for monitored apps, scroll events, usage sessions, schedules, focus sessions, and journal entries.
- Accessibility service skeleton for foreground app sessions, time blocks, scroll detection, partial short-video blocking, breathing/session-picker overlays, and block overlays.
- Settings backed by DataStore for breathing, scroll, blocking, appearance, focus, journal, and security defaults.
- Deep Focus, reports, journal, home widget, quick settings tile, and WorkManager hooks.

## F-Droid Notes

The app does not request `INTERNET`. Dependency audits should keep Firebase, GMS, proprietary analytics, ad SDKs, and runtime code downloads out of the build.

Useful local checks:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:assembleRelease
gradle :app:dependencies
```
