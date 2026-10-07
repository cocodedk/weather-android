package dk.cocode.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
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

        val ticket = publisher.begin()
        requested.forEach { manager.updateAppWidget(it, WidgetViews.loading(context)) }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob()).launch {
            try {
                val store = WeatherStore(appContext)
                val found = loadForWidget({ store.prefs.first() }, ForecastRepository(store))
                publisher.publish(ticket, found, { store.prefs.first() }) { draw ->
                    val views = when (draw) {
                        WidgetLoad.NoPlace -> WidgetViews.empty(appContext)
                        // No network and no cache for this place. Say so rather than
                        // leaving a spinner on the home screen forever.
                        is WidgetLoad.Unavailable ->
                            WidgetViews.empty(appContext, appContext.getString(R.string.widget_unavailable))
                        is WidgetLoad.Ready -> WidgetViews.forecast(
                            appContext,
                            draw.place,
                            draw.loaded.forecast,
                            unitsFor(
                                appContext.resources,
                                imperial = draw.imperial,
                                use24Hour = DateFormat.is24HourFormat(appContext),
                            ),
                            draw.loaded.stale,
                        )
                    }
                    // Every widget, not just the ones this refresh was asked about: an older
                    // refresh for other widgets may have been dropped in favour of this one.
                    ids(appContext, manager).forEach { manager.updateAppWidget(it, views) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** One for the whole process: refreshes from every broadcast take their turn through it. */
        private val publisher = WidgetPublisher()

        const val ACTION_REFRESH = "dk.cocode.weather.widget.REFRESH"

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
