package dk.cocode.weather.widget

/**
 * Remembers which widgets have had a full view drawn since the app was last installed or replaced.
 * Android clears a widget's cached views when the app is updated or reinstalled (even at the same
 * version) but keeps its options, so a plain "drawn" flag would survive while the host is back on the
 * initial layout, which has no click actions. The mark is therefore the time of the install,
 * [installedAt] (PackageInfo.lastUpdateTime), and a widget counts as drawn only when its mark equals
 * it: after any install every widget looks never drawn until something is drawn on it. [read] gives
 * the install time a widget was last drawn at, or [NEVER].
 */
class DrawnMarks(
    private val installedAt: Long,
    private val read: (Int) -> Long,
    private val write: (Int, Long) -> Unit,
) {
    fun isDrawn(id: Int): Boolean = read(id) == installedAt

    fun markDrawn(id: Int) {
        if (!isDrawn(id)) write(id, installedAt)
    }

    companion object {
        const val NEVER = -1L
    }
}
