# Architecture

## Shape

```
ui/          Compose screens + ViewModels
  ├── home, explore, activity, show, player, settings
  ├── components/   EpisodeRow, MiniPlayer, ShowArt, formatters
  └── theme/        Material 3 scheme, type scale, shapes
domain/      Podcast, Episode, NowPlaying — plain Kotlin, no Android types
data/
  ├── local/        Room entities, DAOs, mappers
  ├── remote/       FeedParser, FeedService, SearchApi, Opml
  └── repository/   PodcastRepository, EpisodeRepository, DownloadRepository
playback/    PlaybackService (Media3), PlayerConnection (the UI's handle on it)
sync/        WorkManager jobs + new-episode notifications
di/          Hilt module
```

One module. The app is small enough that splitting it into `core:*` and
`feature:*` would cost more in Gradle wiring than it returns.

## Identity

A show is its **feed URL**. An episode is its **GUID**, falling back to the
enclosure URL when a feed omits `<guid>` — which many do. There is no catalog ID,
because there is no catalog: any RSS feed works.

## The refresh merge

`PodcastRepository.mergeEpisodes` is the one place feed data meets local data.
Rules:

1. `insertAll` uses `OnConflictStrategy.IGNORE`, so an episode already in the DB
   is never overwritten wholesale.
2. Rows that conflicted (`rowId == -1`) get `updateMetadata`, which touches title,
   description, audio URL, artwork, date and duration — and nothing else.
3. Playback position, completion, archive and download state are therefore never
   clobbered by a publisher re-writing their feed.

`FeedService` sends `If-None-Match` / `If-Modified-Since` from stored validators,
so refreshing 50 subscriptions costs 50 conditional GETs and almost no bytes.

## Playback

`PlaybackService` owns the only `ExoPlayer`. The UI never touches it directly;
`PlayerConnection` holds a `MediaController` and publishes a single `NowPlaying`
`StateFlow` that both the mini player and the full player render from.

Position is persisted every 5 seconds and on every pause. "Finished" is not
`STATE_ENDED` alone — `Episode.effectivelyFinished` also counts ≥95% or under 30
seconds remaining, matching how the original marked things played.

## Downloads

`DownloadRepository` writes to `filesDir/episodes/<feed hash>/<guid hash>.<ext>`,
so a show's files can be dropped as a unit. `Episode.playbackUri()` returns the
local path when the file is there and the remote URL otherwise, which is the only
place playback needs to know a download exists.

Retention rules run in `AutoDownloadWorker` after each download pass.

## Background work

Two periodic jobs, both unique:

- `RefreshWorker` — every N hours (user setting), needs any network. Notifies about
  genuinely new episodes, using the insert result to tell new from re-parsed.
- `AutoDownloadWorker` — every 6 hours, constrained to `UNMETERED` when
  "Download over Wi-Fi only" is on, plus `requiresStorageNotLow`.

`PodcastsApplication` re-schedules both whenever the relevant settings change.

## Testing

Unit tests cover the parts with real logic: feed parsing (durations, dates, HTML,
missing GUIDs, artwork inheritance), the finished/started/progress rules, and the
display formatters. Everything else is either Compose layout or a thin DAO.
