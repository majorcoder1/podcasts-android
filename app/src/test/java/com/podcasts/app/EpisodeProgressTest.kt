package com.podcasts.app

import com.podcasts.app.domain.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeProgressTest {

    private fun episode(position: Long, duration: Long = 60 * 60 * 1000L) = Episode(
        guid = "g",
        feedUrl = "f",
        title = "t",
        description = "",
        audioUrl = "https://example.com/a.mp3",
        imageUrl = null,
        publishedAt = 0L,
        durationMs = duration,
        positionMs = position,
    )

    @Test
    fun `counts as finished past 95 percent`() {
        assertTrue(episode(position = 57 * 60 * 1000L).effectivelyFinished)
        assertFalse(episode(position = 50 * 60 * 1000L).effectivelyFinished)
    }

    @Test
    fun `counts as finished inside the last 30 seconds`() {
        val almostDone = episode(position = 175_000L, duration = 180_000L)
        assertTrue(almostDone.effectivelyFinished)
    }

    @Test
    fun `an untouched episode has not started`() {
        assertFalse(episode(position = 0L).hasStarted)
        assertTrue(episode(position = 5_000L).hasStarted)
    }

    @Test
    fun `progress is clamped`() {
        assertEquals(0f, episode(position = 0L).progress, 0.001f)
        assertEquals(1f, episode(position = 99 * 60 * 1000L).progress, 0.001f)
    }

    @Test
    fun `remaining never goes negative`() {
        assertEquals(0L, episode(position = 99 * 60 * 1000L).remainingMs)
    }
}
