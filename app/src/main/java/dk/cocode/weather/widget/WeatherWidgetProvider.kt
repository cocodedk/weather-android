package dk.cocode.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.RemoteViews
import dk.cocode.weather.R
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.WeatherStore
import dk.cocode.weather.ui.unitsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home screen widget showing current conditions for the location selected in the
 * app. There is no per-widget configuration on purpose: the widget and the app
 * always agree about where you are looking, and there is nothing to set up.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            refresh(context, manager, ids(context, manager))
        }
    }

    /**
     * Fetches and repaints. `goAsync()` holds the broadcast alive past the return
     * of onReceive — without it the process can be killed mid-request and the
     * widget silently keeps showing yesterday's numbers.
     */
    private fun refresh(context: Context, manager: AppWidgetManager, requested: IntArray) {
        if (requested.isEmpty()) return

        // The time the broadcast allows runs from here, not from when the coroutine gets going.
        val startedNanos = System.nanoTime()
        val pending = goAsync()
        val ticket = publisher.begin()

        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob()).launch {
            try {
                val store = WeatherStore(appContext)
                // Kept in each widget's own options: system calls, no stored data to read. The time of the
                // install is part of the mark because installing the app clears a widget's views (even at
                // the same version) but keeps its options.
                val installedAt = appContext.packageManager.getPackageInfo(appContext.packageName, 0).lastUpdateTime
                val marks = DrawnMarks(
                    installedAt,
                    read = { id -> manager.getAppWidgetOptions(id).getLong(DRAWN_AT_OPTION, DrawnMarks.NEVER) },
                    write = { id, at ->
                        manager.updateAppWidgetOptions(id, Bundle().apply { putLong(DRAWN_AT_OPTION, at) })
                    },
                )
                val surface = object : WidgetSurface<RemoteViews> {
                    override fun build(found: WidgetLoad): RemoteViews = when (found) {
                        WidgetLoad.NoPlace -> WidgetViews.empty(appContext)
                        // No network and no cache for this place. Say so rather than
                        // leaving the home screen without an answer.
                        is WidgetLoad.Unavailable ->
                            WidgetViews.empty(appContext, appContext.getString(R.string.widget_unavailable))
                        is WidgetLoad.Ready -> WidgetViews.forecast(
                            appContext,
                            found.place,
                            found.loaded.forecast,
                            unitsFor(
                                appContext.resources,
                                imperial = found.imperial,
                                use24Hour = DateFormat.is24HourFormat(appContext),
                            ),
                            found.loaded.stale,
                        )
                    }

                    // The "Tap to open Weather" view, which unlike the layout Android first shows has its clicks wired.
                    override fun placeholder(): RemoteViews = WidgetViews.empty(appContext)

                    override fun allIds(): IntArray = ids(appContext, manager)

                    override fun isDrawn(id: Int): Boolean = marks.isDrawn(id)

                    override fun update(id: Int, views: RemoteViews) {
                        manager.updateAppWidget(id, views)
                        marks.markDrawn(id)
                    }
                }
                refreshWidget(ticket, publisher, { store.prefs.first() }, ForecastRepository(store), surface, startedNanos)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** One for the whole process: refreshes from every broadcast take their turn through it. */
        private val publisher = WidgetPublisher()

        const val ACTION_REFRESH = "dk.cocode.weather.widget.REFRESH"

        /** Set in a widget's options to the install time (lastUpdateTime) of the app that last drew a full view on it. */
        private const val DRAWN_AT_OPTION = "dk.cocode.weather.drawnAt"

        private fun ids(context: Context, manager: AppWidgetManager): IntArray =
            manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))

        /**
         * Called by the app when the selected place, units or forecast change, so the
         * widget does not sit on stale numbers until its next 30-minute tick.
         */
        fun notifyDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            if (ids(context, manager).isEmpty()) return
            context.sendBroadcast(
                Intent(context, WeatherWidgetProvider::class.java).setAction(ACTION_REFRESH)
            )
        }

        /** True when the launcher can show a "pin this widget" dialog (API 26+). */
        fun canPin(context: Context): Boolean =
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        /**
         * Asks the launcher to offer the widget. Saves the user hunting through the
         * long-press widget drawer, which is where most people never look.
         */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(
                ComponentName(context, WeatherWidgetProvider::class.java), null, null,
            )
        }
    }
}
