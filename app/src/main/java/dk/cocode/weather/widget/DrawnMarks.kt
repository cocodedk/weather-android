package dk.cocode.weather.widget

/**
 * Remembers which widgets have had a full view drawn at the running app [version]. Android clears a
 * widget's cached views when the app is updated but keeps its options, so a plain "drawn" flag would
 * survive while the host is back on the initial layout, which has no click actions. Keeping the
 * version in the mark makes a widget that was last drawn by an older version look like one that has
 * never been drawn. [read] gives the version a widget was last drawn at, or [NEVER].
 */
class DrawnMarks(
    private val version: Long,
    private val read: (Int) -> Long,
    private val write: (Int, Long) -> Unit,
) {
    fun isDrawn(id: Int): Boolean = read(id) == version

    fun markDrawn(id: Int) {
        if (!isDrawn(id)) write(id, version)
    }

    companion object {
        const val NEVER = -1L
    }
}
