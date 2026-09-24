package com.podcasts.app.ui

import android.net.Uri

/** Routes. Feed URLs are the show identity, so they are URL-encoded in the path. */
object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val ACTIVITY = "activity"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val PLAYER = "player"

    private const val SHOW_BASE = "show"
    const val SHOW = "$SHOW_BASE/{feedUrl}"
    fun show(feedUrl: String) = "$SHOW_BASE/${Uri.encode(feedUrl)}"

    private const val EPISODE_BASE = "episode"
    const val EPISODE = "$EPISODE_BASE/{guid}"
    fun episode(guid: String) = "$EPISODE_BASE/${Uri.encode(guid)}"
}

/** What a launcher long-press asked for. */
enum class Shortcut { RESUME, NEW_EPISODES, QUEUE, EXPLORE }
