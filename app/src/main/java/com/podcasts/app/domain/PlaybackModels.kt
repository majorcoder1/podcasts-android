package com.podcasts.app.domain

data class NowPlaying(
    val episode: Episode? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val sleepTimerEndsAt: Long? = null,
    val sleepAtEndOfEpisode: Boolean = false,
) {
    val hasMedia: Boolean get() = episode != null
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** The speeds the original speed dialog offered. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 3.0f)

/** Sleep-timer presets, in minutes. `null` = end of episode. */
val SLEEP_TIMER_PRESETS = listOf(5, 10, 15, 30, 45, 60)
