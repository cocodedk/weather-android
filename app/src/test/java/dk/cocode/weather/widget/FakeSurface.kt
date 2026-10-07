package dk.cocode.weather.widget

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * A widget surface that records what each widget was told to draw, as (id, text) pairs, in order.
 * Every widget counts as already drawn (at version 1, the running version) except the ones named
 * in [neverDrawn], like a widget that has just been added; pass [marks] to control versions. An
 * update marks a widget as drawn.
 */
class FakeSurface(
    private val ids: IntArray,
    /** Runs before each update, to stall a refresh in the middle of drawing. */
    private val beforeUpdate: () -> Unit = {},
    private val log: MutableList<Pair<Int, String>> = Collections.synchronizedList(mutableListOf()),
    neverDrawn: Set<Int> = emptySet(),
    private val marks: DrawnMarks = versionedMarks(running = 1L, drawnAt = ids.filter { it !in neverDrawn }.associateWith { 1L }),
) : WidgetSurface<String> {
    val updates: List<Pair<Int, String>> get() = log.toList()

    override fun build(found: WidgetLoad): String = when (found) {
        WidgetLoad.NoPlace -> "nothing saved"
        is WidgetLoad.Unavailable -> "unavailable: ${found.place.name}"
        is WidgetLoad.Ready -> found.place.name + if (found.loaded.stale) " (saved)" else ""
    }

    override fun placeholder(): String = "placeholder"

    override fun allIds(): IntArray = ids

    override fun isDrawn(id: Int): Boolean = marks.isDrawn(id)

    override fun update(id: Int, views: String) {
        beforeUpdate()
        log += id to views
        marks.markDrawn(id)
    }

    companion object {
        /** Marks kept in a map instead of a widget's options: [drawnAt] is the version each widget was last drawn at. */
        fun versionedMarks(running: Long, drawnAt: Map<Int, Long>): DrawnMarks {
            val store = ConcurrentHashMap(drawnAt)
            return DrawnMarks(running, { id -> store[id] ?: DrawnMarks.NEVER }, { id, v -> store[id] = v })
        }
    }
}
