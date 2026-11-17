/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.remoteviews.presenters

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import org.breezyweather.background.receiver.widget.WidgetSplitForecastProvider
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.common.extensions.getFormattedMediumDayAndMonth
import org.breezyweather.common.extensions.getHour
import org.breezyweather.common.extensions.getWeek
import org.breezyweather.common.options.NotificationTextColor
import org.breezyweather.common.source.PollenIndexSource
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.domain.settings.ConfigStore
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.domain.weather.model.getWeek
import org.breezyweather.domain.weather.model.isToday
import org.breezyweather.remoteviews.Widgets
import org.breezyweather.ui.theme.resource.ResourceHelper
import org.breezyweather.ui.theme.resource.ResourcesProviderFactory
import org.breezyweather.unit.formatting.UnitWidth
import java.util.Date

object SplitForecastWidgetIMP : AbstractRemoteViewsPresenter() {
    private const val APPEARANCE_INITIALIZED_KEY = "split_forecast_appearance_initialized"

    /** The previous version saved 'none' / 'light' without exposing color settings.
     *  Preserve the original Material You card when upgrading those widgets.
     */
    fun ensureAppearanceDefaults(context: Context) {
        val store = ConfigStore(context, context.getString(R.string.sp_widget_split_forecast_setting))
        if (!store.getBoolean(APPEARANCE_INITIALIZED_KEY, false)) {
            store.edit()
                .putString(context.getString(R.string.key_card_style), "auto")
                .putString(context.getString(R.string.key_text_color), "dark")
                .putBoolean(APPEARANCE_INITIALIZED_KEY, true)
                .apply()
        }
    }

    fun isInUse(context: Context): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, WidgetSplitForecastProvider::class.java)).isNotEmpty()

    fun updateWidgetView(context: Context, location: Location?, pollenIndexSource: PollenIndexSource?) {
        ensureAppearanceDefaults(context)
        val config = getWidgetConfig(context, context.getString(R.string.sp_widget_split_forecast_setting))
        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, WidgetSplitForecastProvider::class.java),
            getRemoteViews(
                context, location, config.cardStyle, config.cardAlpha, config.textColor, config.textSize,
                config.hideSubtitle, config.subtitleData, pollenIndexSource
            )
        )
    }

    /** All split-widget rendering lives here; the Material You presenter is untouched. */
    fun getRemoteViews(
        context: Context,
        location: Location?,
        cardStyle: String?,
        cardAlpha: Int,
        textColor: String?,
        textSize: Int,
        hideSubtitle: Boolean,
        subtitleData: String?,
        pollenIndexSource: PollenIndexSource?,
    ): RemoteViews {
        val color = AbstractRemoteViewsPresenter.WidgetColor(
            context, cardStyle ?: "auto", textColor ?: "dark", location?.isDaylight ?: true
        )
        val views = buildForecastViews(context, location, color.minimalIconColor)
        if (color.showCard) {
            // Preserve the current Material You card in the app-following mode.
            views.setImageViewResource(
                R.id.widget_split_forecast_card,
                if (cardStyle == null || cardStyle == "auto") R.drawable.widget_m3_background
                else AbstractRemoteViewsPresenter.getCardBackgroundId(color)
            )
            views.setInt(
                R.id.widget_split_forecast_card, "setImageAlpha", (cardAlpha.coerceIn(0, 100) * 255) / 100
            )
        } else {
            views.setViewVisibility(R.id.widget_split_forecast_card, View.GONE)
        }

        applyAppearance(views, color, textSize)
        applyAirQuality(views, context, location, pollenIndexSource)

        if (location != null) {
            val date = Date()
            views.setTextViewText(
                R.id.widget_split_forecast_date,
                date.getFormattedMediumDayAndMonth(location, context) + " " + date.getWeek(location, context)
            )
            views.setTextViewText(
                R.id.widget_split_forecast_subtitle,
                DayWeekWidgetIMP.getTimeText(
                    context, location, "split", subtitleData,
                    SettingsManager.getInstance(context).getTemperatureUnit(context), pollenIndexSource
                )
            )
            if (subtitleData == "lunar") {
                views.setOnClickPendingIntent(
                    R.id.widget_split_forecast_subtitle,
                    AbstractRemoteViewsPresenter.getCalendarPendingIntent(
                        context, Widgets.SPLIT_FORECAST_PENDING_INTENT_CODE_CALENDAR
                    )
                )
            }
        }
        views.setViewVisibility(
            R.id.widget_split_forecast_subtitle, if (hideSubtitle) View.GONE else View.VISIBLE
        )
        return views
    }

    private fun applyAirQuality(
        views: RemoteViews,
        context: Context,
        location: Location?,
        pollenIndexSource: PollenIndexSource?,
    ) {
        // Use DayWeek's existing AQI display formatting; don't duplicate its calculation.
        val aqi = location?.takeIf { it.weather?.current?.airQuality?.isIndexValid == true }?.let {
            DayWeekWidgetIMP.getTimeText(
                context, it, "split", "aqi",
                SettingsManager.getInstance(context).getTemperatureUnit(context), pollenIndexSource
            )
        }
        if (aqi.isNullOrBlank()) {
            views.setViewVisibility(R.id.widget_split_forecast_air_quality, View.GONE)
        } else {
            views.setTextViewText(
                R.id.widget_split_forecast_air_quality,
                context.getString(R.string.air_quality) + " " + aqi
            )
            views.setViewVisibility(R.id.widget_split_forecast_air_quality, View.VISIBLE)
        }
    }

    private fun applyAppearance(
        views: RemoteViews,
        color: AbstractRemoteViewsPresenter.WidgetColor,
        textSize: Int,
    ) {
        val primary = color.textColor
        val secondary = Color.argb(
            (Color.alpha(primary) * 0.70f).toInt(),
            Color.red(primary), Color.green(primary), Color.blue(primary)
        )
        val primaryIds = intArrayOf(
            R.id.widget_material_you_forecast_currentTemperature,
            R.id.widget_material_you_forecast_city,
            R.id.widget_material_you_forecast_hour_1, R.id.widget_material_you_forecast_hour_2,
            R.id.widget_material_you_forecast_hour_3, R.id.widget_material_you_forecast_hour_4,
            R.id.widget_material_you_forecast_hour_5,
            R.id.widget_material_you_forecast_hourlyTemperature_1,
            R.id.widget_material_you_forecast_hourlyTemperature_2,
            R.id.widget_material_you_forecast_hourlyTemperature_3,
            R.id.widget_material_you_forecast_hourlyTemperature_4,
            R.id.widget_material_you_forecast_hourlyTemperature_5,
            R.id.widget_material_you_forecast_week_1, R.id.widget_material_you_forecast_week_2,
            R.id.widget_material_you_forecast_week_3, R.id.widget_material_you_forecast_week_4,
            R.id.widget_material_you_forecast_week_5,
            R.id.widget_material_you_forecast_dayTemperature_1,
            R.id.widget_material_you_forecast_dayTemperature_2,
            R.id.widget_material_you_forecast_dayTemperature_3,
            R.id.widget_material_you_forecast_dayTemperature_4,
            R.id.widget_material_you_forecast_dayTemperature_5
        )
        primaryIds.forEach { views.setTextColor(it, primary) }
        val secondaryIds = intArrayOf(
            R.id.widget_material_you_forecast_weatherText,
            R.id.widget_split_forecast_air_quality,
            R.id.widget_split_forecast_date,
            R.id.widget_split_forecast_subtitle,
            R.id.widget_material_you_forecast_nightTemperature_1,
            R.id.widget_material_you_forecast_nightTemperature_2,
            R.id.widget_material_you_forecast_nightTemperature_3,
            R.id.widget_material_you_forecast_nightTemperature_4,
            R.id.widget_material_you_forecast_nightTemperature_5,
            R.id.widget_split_forecast_temp_separator_1, R.id.widget_split_forecast_temp_separator_2,
            R.id.widget_split_forecast_temp_separator_3, R.id.widget_split_forecast_temp_separator_4,
            R.id.widget_split_forecast_temp_separator_5
        )
        secondaryIds.forEach { views.setTextColor(it, secondary) }

        if (textSize != 100) {
            val scale = textSize.coerceIn(50, 250) / 100f
            fun setScaledSize(id: Int, baseSp: Float) {
                views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, baseSp * scale)
            }
            setScaledSize(R.id.widget_material_you_forecast_currentTemperature, 36f)
            setScaledSize(R.id.widget_material_you_forecast_city, 15f)
            setScaledSize(R.id.widget_material_you_forecast_weatherText, 12f)
            setScaledSize(R.id.widget_split_forecast_air_quality, 12f)
            setScaledSize(R.id.widget_split_forecast_date, 12f)
            setScaledSize(R.id.widget_split_forecast_subtitle, 12f)
            val forecastTextIds = listOf(
                intArrayOf(
                    R.id.widget_material_you_forecast_hour_1, R.id.widget_material_you_forecast_hour_2,
                    R.id.widget_material_you_forecast_hour_3, R.id.widget_material_you_forecast_hour_4,
                    R.id.widget_material_you_forecast_hour_5
                ) to 12f,
                intArrayOf(
                    R.id.widget_material_you_forecast_hourlyTemperature_1,
                    R.id.widget_material_you_forecast_hourlyTemperature_2,
                    R.id.widget_material_you_forecast_hourlyTemperature_3,
                    R.id.widget_material_you_forecast_hourlyTemperature_4,
                    R.id.widget_material_you_forecast_hourlyTemperature_5
                ) to 13f,
                intArrayOf(
                    R.id.widget_material_you_forecast_week_1, R.id.widget_material_you_forecast_week_2,
                    R.id.widget_material_you_forecast_week_3, R.id.widget_material_you_forecast_week_4,
                    R.id.widget_material_you_forecast_week_5
                ) to 12f,
                intArrayOf(
                    R.id.widget_material_you_forecast_dayTemperature_1,
                    R.id.widget_material_you_forecast_dayTemperature_2,
                    R.id.widget_material_you_forecast_dayTemperature_3,
                    R.id.widget_material_you_forecast_dayTemperature_4,
                    R.id.widget_material_you_forecast_dayTemperature_5
                ) to 11f,
                intArrayOf(
                    R.id.widget_material_you_forecast_nightTemperature_1,
                    R.id.widget_material_you_forecast_nightTemperature_2,
                    R.id.widget_material_you_forecast_nightTemperature_3,
                    R.id.widget_material_you_forecast_nightTemperature_4,
                    R.id.widget_material_you_forecast_nightTemperature_5
                ) to 11f,
                intArrayOf(
                    R.id.widget_split_forecast_temp_separator_1, R.id.widget_split_forecast_temp_separator_2,
                    R.id.widget_split_forecast_temp_separator_3, R.id.widget_split_forecast_temp_separator_4,
                    R.id.widget_split_forecast_temp_separator_5
                ) to 11f
            )
            forecastTextIds.forEach { (ids, baseSp) ->
                ids.forEach { id -> setScaledSize(id, baseSp) }
            }
        }
    }

    private fun buildForecastViews(
        context: Context,
        location: Location?,
        iconColor: NotificationTextColor,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_split_forecast)
        val weather = location?.weather ?: return views
        val dayTime = location.isDaylight
        val provider = ResourcesProviderFactory.newInstance
        val settings = SettingsManager.getInstance(context)
        val temperatureUnit = settings.getTemperatureUnit(context)
        val monochrome = settings.isWidgetUsingMonochromeIcons

        views.setTextViewText(R.id.widget_material_you_forecast_city, location.getPlace(context))

        weather.current?.weatherCode?.let {
            views.setViewVisibility(R.id.widget_material_you_forecast_currentIcon, View.VISIBLE)
            views.setImageViewUri(
                R.id.widget_material_you_forecast_currentIcon,
                ResourceHelper.getWidgetNotificationIconUri(provider, it, dayTime, monochrome, iconColor)
            )
        } ?: views.setViewVisibility(R.id.widget_material_you_forecast_currentIcon, View.INVISIBLE)

        views.setTextViewText(
            R.id.widget_material_you_forecast_currentTemperature,
            weather.current?.temperature?.temperature?.formatMeasure(
                context, temperatureUnit, valueWidth = UnitWidth.NARROW, unitWidth = UnitWidth.NARROW
            )
        )
        views.setTextViewText(R.id.widget_material_you_forecast_weatherText, weather.current?.weatherText)

        val hours = arrayOf(
            intArrayOf(R.id.widget_material_you_forecast_hour_1, R.id.widget_material_you_forecast_hourlyIcon_1, R.id.widget_material_you_forecast_hourlyTemperature_1),
            intArrayOf(R.id.widget_material_you_forecast_hour_2, R.id.widget_material_you_forecast_hourlyIcon_2, R.id.widget_material_you_forecast_hourlyTemperature_2),
            intArrayOf(R.id.widget_material_you_forecast_hour_3, R.id.widget_material_you_forecast_hourlyIcon_3, R.id.widget_material_you_forecast_hourlyTemperature_3),
            intArrayOf(R.id.widget_material_you_forecast_hour_4, R.id.widget_material_you_forecast_hourlyIcon_4, R.id.widget_material_you_forecast_hourlyTemperature_4),
            intArrayOf(R.id.widget_material_you_forecast_hour_5, R.id.widget_material_you_forecast_hourlyIcon_5, R.id.widget_material_you_forecast_hourlyTemperature_5)
        )
        hours.forEachIndexed { i, ids ->
            // Same five 2-hour increments as the previous split widget.
            val hourly = weather.nextHourlyForecast.getOrNull(i * 2)
            views.setTextViewText(ids[0], hourly?.date?.getHour(location, context))
            hourly?.weatherCode?.let {
                views.setViewVisibility(ids[1], View.VISIBLE)
                views.setImageViewUri(
                    ids[1],
                    ResourceHelper.getWidgetNotificationIconUri(
                        provider, it, hourly?.isDaylight ?: dayTime, monochrome, iconColor
                    )
                )
            } ?: views.setViewVisibility(ids[1], View.INVISIBLE)
            views.setTextViewText(
                ids[2],
                hourly?.temperature?.temperature?.formatMeasure(
                    context, temperatureUnit, valueWidth = UnitWidth.NARROW, unitWidth = UnitWidth.NARROW
                )
            )
        }

        val days = arrayOf(
            intArrayOf(R.id.widget_material_you_forecast_week_1, R.id.widget_material_you_forecast_dayIcon_1, R.id.widget_material_you_forecast_dayTemperature_1, R.id.widget_material_you_forecast_nightTemperature_1),
            intArrayOf(R.id.widget_material_you_forecast_week_2, R.id.widget_material_you_forecast_dayIcon_2, R.id.widget_material_you_forecast_dayTemperature_2, R.id.widget_material_you_forecast_nightTemperature_2),
            intArrayOf(R.id.widget_material_you_forecast_week_3, R.id.widget_material_you_forecast_dayIcon_3, R.id.widget_material_you_forecast_dayTemperature_3, R.id.widget_material_you_forecast_nightTemperature_3),
            intArrayOf(R.id.widget_material_you_forecast_week_4, R.id.widget_material_you_forecast_dayIcon_4, R.id.widget_material_you_forecast_dayTemperature_4, R.id.widget_material_you_forecast_nightTemperature_4),
            intArrayOf(R.id.widget_material_you_forecast_week_5, R.id.widget_material_you_forecast_dayIcon_5, R.id.widget_material_you_forecast_dayTemperature_5, R.id.widget_material_you_forecast_nightTemperature_5)
        )
        val useDayIcon = AbstractRemoteViewsPresenter.isWeekIconDaytime(
            settings.widgetWeekIconMode, dayTime
        )
        days.forEachIndexed { i, ids ->
            val daily = weather.dailyForecastStartingToday.getOrNull(i)
            views.setTextViewText(
                ids[0],
                daily?.let {
                    if (it.isToday(location)) context.getString(R.string.daily_today_short)
                    else it.getWeek(location, context)
                }
            )
            (if (useDayIcon) daily?.day?.weatherCode else daily?.night?.weatherCode)?.let {
                views.setViewVisibility(ids[1], View.VISIBLE)
                views.setImageViewUri(
                    ids[1], ResourceHelper.getWidgetNotificationIconUri(
                        provider, it, useDayIcon, monochrome, iconColor
                    )
                )
            } ?: views.setViewVisibility(ids[1], View.INVISIBLE)
            views.setTextViewText(
                ids[2],
                daily?.day?.temperature?.temperature?.formatMeasure(
                    context, temperatureUnit, valueWidth = UnitWidth.NARROW, unitWidth = UnitWidth.NARROW
                )
            )
            views.setTextViewText(
                ids[3],
                daily?.night?.temperature?.temperature?.formatMeasure(
                    context, temperatureUnit, valueWidth = UnitWidth.NARROW, unitWidth = UnitWidth.NARROW
                )
            )
        }

        views.setOnClickPendingIntent(
            android.R.id.background,
            AbstractRemoteViewsPresenter.getWeatherPendingIntent(
                context, location, Widgets.SPLIT_FORECAST_PENDING_INTENT_CODE_WEATHER
            )
        )
        return views
    }
}
