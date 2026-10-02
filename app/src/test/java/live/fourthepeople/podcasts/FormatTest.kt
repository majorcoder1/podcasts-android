package live.fourthepeople.podcasts

import live.fourthepeople.podcasts.ui.components.formatClock
import live.fourthepeople.podcasts.ui.components.formatDuration
import live.fourthepeople.podcasts.ui.components.formatEpisodeDate
import live.fourthepeople.podcasts.ui.components.formatSpeed
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `durations read the way the app shows them`() {
        assertEquals("48 min", formatDuration(48 * 60_000L))
        assertEquals("1 hr 4 min", formatDuration(64 * 60_000L))
        assertEquals("2 hr", formatDuration(120 * 60_000L))
        assertEquals("", formatDuration(0L))
    }

    @Test
    fun `clock drops the hour when it is zero`() {
        assertEquals("2:05", formatClock(125_000L))
        assertEquals("1:02:05", formatClock(3_725_000L))
    }

    @Test
    fun `recent dates read as today and yesterday`() {
        val now = System.currentTimeMillis()
        assertEquals("Today", formatEpisodeDate(now, now))
        assertEquals("Yesterday", formatEpisodeDate(now - TimeUnit.DAYS.toMillis(1), now))
    }

    @Test
    fun `speeds drop a trailing zero`() {
        assertEquals("1x", formatSpeed(1.0f))
        assertEquals("1.5x", formatSpeed(1.5f))
    }
}
