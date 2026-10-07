package dk.cocode.weather.widget

import java.util.Collections

/**
 * A widget surface that records what each widget was told to draw, as (id, text) pairs, in order.
 * Every widget counts as already drawn except the ones named in [neverDrawn], like a widget that
 * has just been added; an update marks a widget as drawn.
 */
class FakeSurface(
    private val ids: IntArray,
    /** Runs before each update, to stall a refresh in the middle of drawing. */
    private val beforeUpdate: () -> Unit = {},
    private val log: MutableList<Pair<Int, String>> = Collections.synchronizedList(mutableListOf()),
    neverDrawn: Set<Int> = emptySet(),
) : WidgetSurface<String> {
    private val drawn: MutableSet<Int> = Collections.synchronizedSet((ids.toSet() - neverDrawn).toMutableSet())

    val updates: List<Pair<Int, String>> get() = log.toList()

    override fun build(found: WidgetLoad): String = when (found) {
        WidgetLoad.NoPlace -> "nothing saved"
        is WidgetLoad.Unavailable -> "unavailable: ${found.place.name}"
        is WidgetLoad.Ready -> found.place.name + if (found.loaded.stale) " (saved)" else ""
    }

    override fun placeholder(): String = "placeholder"

    override fun allIds(): IntArray = ids

    override fun isDrawn(id: Int): Boolean = id in drawn

    override fun update(id: Int, views: String) {
        beforeUpdate()
        log += id to views
        drawn += id
    }
}
