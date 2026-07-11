<div align="center">

# ⚓ Halter

**Stop the scroll before it starts.**

A free, open-source, privacy-respecting digital wellbeing app for Android that limits *what actually gets people stuck* — endless Reels and Shorts — not just app open time.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![F-Droid](https://img.shields.io/f-droid/v/com.ujwal.Halter)](https://f-droid.org/packages/com.ujwal.Halter/)


<!-- To be REplaced with real screenshots (IN PUBLIC RELEASE) -->
<img src=".github/screenshots/dashboard.png" width="200"/> <img src=".github/screenshots/breathing.png" width="200"/> <img src=".github/screenshots/reports.png" width="200"/>

</div>

---

## What is Halter?

Most screen-time apps stop at "you've used this app for 40 minutes today." That's not usually where the problem is — you can lose an hour to Reels or Shorts in a single sitting without ever *feeling* like 40 minutes passed. Halter limits the actual behavior: **the scroll itself.**

Halter combines three things that normally live in three separate apps:

- **App & schedule blocking** with daily limits, session limits, and instant blocks.
- **Reels/Shorts blocking** — block short-form video in Instagram, YouTube, Facebook, and TikTok.
- **A mandatory breathing pause** before you open anything you've flagged as distracting, followed by an honest, deliberate choice of how long or how much you're actually allowing yourself right now.

No login. No cloud. No ads. No analytics. No network permission at all — Halter has no way to phone home even if it wanted to.

## Features

**Blocking & Limits**
- Instant block, scheduled block, daily time limit, and session time limit — coexisting per app
- Strict Mode: lock in a limit so you can't quietly turn it off mid-urge
- Partial in-app blocking — block just the Reels tab in Instagram, leave the rest of the app usable
- A small, movable on-screen counter for supported apps showing live scroll count / screen time, without covering your content

**The Halter Pause**
- A 15-second (fully adjustable) guided breathing animation before any flagged app opens
- Followed by a deliberate session-limit picker 
- An optional, private, on-device-only reflection prompt: *"What do you actually need this for?"*

**Real Productivity Tools, Not Gimmicks**
- Deep Focus sessions that hard-block distracting apps for a set stretch, no exceptions
- An honest weekly report — real numbers, no vanity badges
- A streak that only counts days you actually stayed under every limit you set
- Home screen widgets and a Quick Settings tile, themed to match your device

**Design**
- Full Material You dynamic color, or pick your own accent
- Material 3 Expressive motion and translucent surfaces
- Complete dark mode support

## Privacy

Halter requests no `INTERNET` permission and contains no third-party SDKs, analytics, or ad frameworks of any kind. Everything it does happens on your device and stays on your device. The permissions it does request — Accessibility, Usage Access, and Display Over Other Apps — are the only technical way to implement blocking and scroll-counting on Android, and are explained plainly during onboarding before you're asked to grant them.

## Installing

<!----<a href="https://f-droid.org/packages/com.ujwal.Halter/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="200"/></a> ---->

Halter is also available as a signed APK from the [Releases](../../releases) page. It intentionally isn't on Google Play.
If INstalling from APK, please turn off Google Play Protect.

## Building from source

```bash
git clone https://github.com/<your-username>/Halter.git
cd Halter
./gradlew assembleDebug
```

Requirements: JDK 17+, Android SDK with `compileSdk` set to the latest stable release. No API keys, no `local.properties` secrets, no signing config needed for a debug build — this project has no backend to configure.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Room · DataStore · WorkManager · Koin · Vico (charts) · Jetpack Glance (widgets)

## Contributing

Issues and PRs are welcome. Before contributing code, please check that any new dependency is FOSS and doesn't reach the network — this is a hard requirement for keeping Halter on F-Droid (see [Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/)).


## License

Halter is licensed under the [GNU General Public License v3.0](LICENSE). You're free to use, study, modify, and share it, including commercially, as long as derivatives stay under the same license.

---

<div align="center">
<sub>Built because the apps designed to take your attention don't have an incentive to give it back. This one does.</sub>
</div>
