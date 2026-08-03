package dk.cocode.weather.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import dk.cocode.weather.MainActivity
import dk.cocode.weather.R
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.Place
import dk.cocode.weather.domain.Units
import dk.cocode.weather.domain.WeatherIcon
import dk.cocode.weather.domain.Wmo

/** Builds the widget's RemoteViews. Pure presentation — no IO, no state. */
object WidgetViews {

    private const val ICON_PX = 132   // 44dp at xxxhdpi, so it stays sharp when scaled down
    private val ACCENT = Color(0xFFFFD257)
    private val INK = Color(0xFFFFFFFF)

    /** Icons whose artwork contains a sun or moon, and so carry the accent colour. */
    private val SKY_ICONS = setOf(
        WeatherIcon.CLEAR, WeatherIcon.CLEAR_NIGHT,
        WeatherIcon.PARTLY, WeatherIcon.PARTLY_NIGHT, WeatherIcon.SHOWERS,
    )

    fun loading(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_weather).apply {
            setTextViewText(R.id.widget_status, context.getString(R.string.widget_loading))
            wireClicks(context, this)
        }

    fun empty(context: Context, message: String? = null): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_weather).apply {
            setTextViewText(R.id.widget_place, context.getString(R.string.app_name))
            setTextViewText(R.id.widget_temp, "--°")
            setTextViewText(R.id.widget_cond, message ?: context.getString(R.string.widget_no_data))
            setTextViewText(R.id.widget_status, "")
            setTextViewText(R.id.widget_range, "")
            wireClicks(context, this)
        }

    fun forecast(
        context: Context,
        place: Place,
        forecast: Forecast,
        units: Units,
        stale: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)
        val current = forecast.current
        val today = forecast.daily.firstOrNull()
        val icon = Wmo.icon(current.weatherCode, current.isDay)

        // Night follows the *location's* is_day, not the phone's theme.
        views.setInt(
            R.id.widget_root, "setBackgroundResource",
            if (current.isDay) R.drawable.widget_background else R.drawable.widget_background_night,
        )

        views.setImageViewBitmap(
            R.id.widget_icon,
            WidgetIcons.render(icon, ICON_PX, if (icon in SKY_ICONS) ACCENT else INK),
        )

        views.setTextViewText(R.id.widget_place, place.name)
        views.setTextViewText(R.id.widget_temp, units.temp(current.temperature) + units.tempUnit())
        views.setTextViewText(R.id.widget_cond, Wmo.label(current.weatherCode))
        views.setTextViewText(
            R.id.widget_range,
            if (today == null) "" else
                "H ${units.temp(today.temperatureMax)}°\nL ${units.temp(today.temperatureMin)}°",
        )
        views.setTextViewText(
            R.id.widget_status,
            if (stale) "Offline · ${units.clock(current.time)}"
            else "Updated ${units.clock(current.time)}",
        )

        wireClicks(context, views)
        return views
    }

    /** Body opens the app; the corner icon forces a refetch. */
    private fun wireClicks(context: Context, views: RemoteViews) {
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        val refresh = Intent(context, WeatherWidgetProvider::class.java)
            .setAction(WeatherWidgetProvider.ACTION_REFRESH)
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(
                context, 1, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
