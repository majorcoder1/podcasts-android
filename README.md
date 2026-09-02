# Podcasts

An Android podcast player that reimplements the feature set and interaction model
of Google Podcasts, which shut down in 2024.

This is original code. Google Podcasts was never open source and none of its code,
assets, or branding are used here — this repo rebuilds the same app from its
observable behaviour, on top of RSS, which is what the original ran on too.

The launcher icon is an original mark — five rounded waveform bars in the app's
blue ramp — with an adaptive-icon foreground, a monochrome layer for Android 13+
themed icons, and a legacy vector for API 24-25. Google's own logo is a
registered trademark and is deliberately not reproduced.

## What it does

**Shell** — three bottom tabs (Home, Explore, Activity) with the mini player docked
above them and a full-screen player that expands from it.

**Home** — your subscriptions in a horizontal strip, then the queue, "Continue
listening", and "New episodes", newest first.

**Explore** — search-as-you-type across the podcast directory, category chips
(News, Comedy, True crime, Sports, …), and subscribing by raw RSS URL.

**Activity** — Queue, Downloads, and History tabs.

**Show page** — art, title, description, Subscribe toggle, sort newest/oldest,
per-show auto-download and notification switches.

**Player** — scrubber, back 10 / forward 30, playback speed (0.5x–3x), sleep timer
with the 5/10/15/30/45/60-minute presets plus "end of episode", skip silence,
and lock-screen and notification controls through a media session.

**Background** — periodic feed refresh, new-episode notifications, auto-download
with a per-show cap, "remove when played" and "remove after N days" retention,
and Wi-Fi-only downloading.

**Data** — OPML import and export, so an export from the original app imports here
directly.

## Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Playback | Media3 (ExoPlayer + MediaSessionService) |
| Storage | Room + DataStore |
| Background work | WorkManager |
| DI | Hilt |
| Network | OkHttp, XmlPullParser for feeds |

Min SDK 24, target SDK 35.

## Build

```bash
./gradlew assembleDebug
```

The Gradle wrapper is committed (Gradle 8.13). If Git drops the execute bit on
`gradlew` when you push from a FAT-formatted drive, restore it with
`git update-index --chmod=+x gradlew`.

Install to a connected device:

```bash
./gradlew installDebug
```

Run the tests:

```bash
./gradlew testDebugUnitTest
```

## Directory search

`SearchApi` uses the free iTunes Search API, which returns the same `feedUrl`
values any podcast crawler indexes and needs no key. It is one class — swap it for
Podcast Index or your own catalog without touching anything else.

## What is not here

- **Cross-device sync.** The original synced positions through a Google account.
  Doing that needs a backend; `EpisodeRepository.savePosition` is the single seam
  where you would push and pull.
- **Cast.** The player has the Cast button; wiring it needs the Cast SDK and a
  receiver app ID.
- **Android Auto / Wear.** `PlaybackService` is a `MediaSessionService`, so the
  browse tree is the only missing piece.

## Licence

Apache 2.0. See [LICENSE](LICENSE).
