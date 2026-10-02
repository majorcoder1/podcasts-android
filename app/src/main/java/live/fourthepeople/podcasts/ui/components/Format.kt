package live.fourthepeople.podcasts.ui.components

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** "1 hr 4 min", "48 min", "9 min" — the phrasing used on episode rows. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours hr $minutes min"
        hours > 0 -> "$hours hr"
        minutes > 0 -> "$minutes min"
        else -> "1 min"
    }
}

/** "12:04" / "1:02:33" — the player's position and duration readouts. */
fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** "8 min left" on a partially played episode. */
fun formatRemaining(ms: Long): String = "${formatDuration(ms)} left"

/** "Today", "Yesterday", "Mar 4" — the date stamp on episode rows. */
fun formatEpisodeDate(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0) return ""
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameYear = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    val dayDiff = daysBetween(then, today)
    return when {
        dayDiff == 0 -> "Today"
        dayDiff == 1 -> "Yesterday"
        dayDiff in 2..6 -> then.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).orEmpty()
        sameYear -> monthDay(then)
        else -> "${monthDay(then)}, ${then.get(Calendar.YEAR)}"
    }
}

private fun monthDay(calendar: Calendar): String {
    val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US).orEmpty()
    return "$month ${calendar.get(Calendar.DAY_OF_MONTH)}"
}

private fun daysBetween(then: Calendar, today: Calendar): Int {
    val a = then.clone() as Calendar
    val b = today.clone() as Calendar
    listOf(a, b).forEach {
        it.set(Calendar.HOUR_OF_DAY, 0)
        it.set(Calendar.MINUTE, 0)
        it.set(Calendar.SECOND, 0)
        it.set(Calendar.MILLISECOND, 0)
    }
    return TimeUnit.MILLISECONDS.toDays(b.timeInMillis - a.timeInMillis).toInt()
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.US, "%.0f MB", bytes / 1_000_000.0)
    bytes > 0 -> String.format(Locale.US, "%.0f KB", bytes / 1000.0)
    else -> ""
}

fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else String.format(Locale.US, "%.1fx", speed)
