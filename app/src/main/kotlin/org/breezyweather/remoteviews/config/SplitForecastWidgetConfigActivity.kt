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

import android.view.View
import android.widget.RemoteViews
import breezyweather.data.location.LocationRepository
import breezyweather.data.weather.WeatherRepository
import breezyweather.domain.location.model.Location
import dagger.hilt.android.AndroidEntryPoint
import org.breezyweather.R
import org.breezyweather.common.options.appearance.CalendarHelper
import org.breezyweather.remoteviews.presenters.SplitForecastWidgetIMP
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

@AndroidEntryPoint
class SplitForecastWidgetConfigActivity : AbstractWidgetConfigActivity() {
    private var locationNow: Location? = null

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
            this, locationNow, cardStyleValueNow, cardAlpha, textColorValueNow, textSize,
            hideSubtitle, subtitleDataValueNow, pollenSource
        )

    override val configStoreName: String
        get() = getString(R.string.sp_widget_split_forecast_setting)
}
