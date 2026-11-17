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

package org.breezyweather.remoteviews.config

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RemoteViews
import breezyweather.data.location.LocationRepository
import breezyweather.data.weather.WeatherRepository
import breezyweather.domain.location.model.Location
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.breezyweather.R
import org.breezyweather.common.options.appearance.CalendarHelper
import org.breezyweather.domain.settings.ConfigStore
import org.breezyweather.remoteviews.presenters.SplitForecastWidgetIMP
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

@AndroidEntryPoint
class SplitForecastWidgetConfigActivity : AbstractWidgetConfigActivity() {
    private var locationNow: Location? = null
    private var currentTemperatureSp = SplitForecastWidgetIMP.DEFAULT_CURRENT_TEMPERATURE_SP
    private var forecastTemperaturePercent = SplitForecastWidgetIMP.DEFAULT_FORECAST_TEMPERATURE_PERCENT

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var weatherRepository: WeatherRepository
    @Inject lateinit var sourceManager: SourceManager

    override suspend fun initLocations() {
        val location = locationRepository.getFirstLocation(withParameters = false)
        locationNow = location?.copy(
            weather = weatherRepository.getWeatherByLocationId(
                location.formattedId,
                withDaily = true,
                withHourly = true,
                withMinutely = false,
                withAlerts = true, // Existing custom subtitle support
                withNormals = false
            )
        )
    }

    override fun initData() {
        super.initData()
        SplitForecastWidgetIMP.ensureAppearanceDefaults(this)
        // Use the existing widget card and text color options while preserving
        // the split widget's original Material You background by default.
        cardStyleValueNow = "auto"
        textColorValueNow = "dark"
        // Existing subtitle options and parser; prefer alternate calendar if it is configured.
        subtitleDataValueNow = if (CalendarHelper.getAlternateCalendarSetting(this) != null) "lunar" else "time"
    }

    override fun initView() {
        super.initView()
        mCardStyleContainer?.visibility = View.VISIBLE
        mCardAlphaContainer?.visibility = View.VISIBLE
        mTextColorContainer?.visibility = View.VISIBLE
        mTextSizeContainer?.visibility = View.VISIBLE
        mHideSubtitleContainer?.visibility = View.VISIBLE
        mSubtitleDataContainer?.visibility = View.VISIBLE

        // Extra controls exist only on this widget, not on the shared widget config screen.
        val saved = ConfigStore(this, configStoreName!!)
        currentTemperatureSp = saved.getInt(
            SplitForecastWidgetIMP.KEY_CURRENT_TEMPERATURE_SP,
            SplitForecastWidgetIMP.DEFAULT_CURRENT_TEMPERATURE_SP
        ).coerceIn(22, 38)
        forecastTemperaturePercent = saved.getInt(
            SplitForecastWidgetIMP.KEY_FORECAST_TEMPERATURE_PERCENT,
            SplitForecastWidgetIMP.DEFAULT_FORECAST_TEMPERATURE_PERCENT
        ).coerceIn(80, 130)
        val extra = LayoutInflater.from(this).inflate(R.layout.widget_split_forecast_size_controls, null)
        val container = findViewById<LinearLayout>(R.id.activity_widget_config_scrollContainer)
        container.addView(extra, container.childCount - 1)
        extra.findViewById<Slider>(R.id.widget_split_current_temperature_size).apply {
            valueFrom = 22f
            valueTo = 38f
            stepSize = 1f
            value = currentTemperatureSp.toFloat()
            setLabelFormatter { "${it.toInt()} sp" }
            addOnChangeListener { _, newValue, fromUser ->
                if (fromUser) {
                    currentTemperatureSp = newValue.toInt()
                    updateHostView()
                }
            }
        }
        extra.findViewById<Slider>(R.id.widget_split_forecast_temperature_size).apply {
            valueFrom = 80f
            valueTo = 130f
            stepSize = 5f
            value = forecastTemperaturePercent.toFloat()
            setLabelFormatter { "${it.toInt()}%" }
            addOnChangeListener { _, newValue, fromUser ->
                if (fromUser) {
                    forecastTemperaturePercent = newValue.toInt()
                    updateHostView()
                }
            }
        }
    }

    override fun saveAdditionalWidgetConfig(editor: ConfigStore.Editor) {
        editor.putInt(SplitForecastWidgetIMP.KEY_CURRENT_TEMPERATURE_SP, currentTemperatureSp)
            .putInt(SplitForecastWidgetIMP.KEY_FORECAST_TEMPERATURE_PERCENT, forecastTemperaturePercent)
    }

    private val pollenSource get() = locationNow?.let { location ->
        sourceManager.getPollenIndexSource(
            (location.pollenSource ?: "").ifEmpty { location.forecastSource }
        )
    }

    override fun updateWidgetView() {
        SplitForecastWidgetIMP.updateWidgetView(this, locationNow, pollenSource)
    }

    override val remoteViews: RemoteViews
        get() = SplitForecastWidgetIMP.getRemoteViews(
            this, locationNow, cardStyleValueNow, cardAlpha, textColorValueNow,
            textSize, hideSubtitle, subtitleDataValueNow, pollenSource,
            currentTemperatureSp, forecastTemperaturePercent
        )

    override val configStoreName: String
        get() = getString(R.string.sp_widget_split_forecast_setting)
}
