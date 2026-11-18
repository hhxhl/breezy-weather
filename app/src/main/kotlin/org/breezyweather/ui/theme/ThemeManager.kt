/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.theme

import android.content.Context
import android.content.res.Configuration
import breezyweather.domain.location.model.Location
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.theme.weatherView.WeatherThemeDelegate
import org.breezyweather.ui.theme.weatherView.materialWeatherView.MaterialWeatherThemeDelegate

class ThemeManager private constructor(
    val weatherThemeDelegate: WeatherThemeDelegate,
) {

    companion object {

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            if (instance == null) {
                synchronized(ThemeManager::class) {
                    if (instance == null) {
                        instance = ThemeManager(
                            weatherThemeDelegate = MaterialWeatherThemeDelegate()
                        )
                    }
                }
            }
            return instance!!
        }

        /** A missing location or missing rise/set data falls back to the system theme. */
        fun hasLocationDayNight(location: Location?): Boolean {
            val sun = location?.weather?.today?.sun
            return sun?.riseDate != null && sun.setDate != null
        }

        fun isLightTheme(
            context: Context,
            location: Location?,
        ) = isLightTheme(
            context = context,
            daylight = if (hasLocationDayNight(location)) location?.isDaylight else null
        )

        /** Four mutually exclusive modes; use the system theme when location data is unavailable. */
        fun isLightTheme(
            context: Context,
            daylight: Boolean?,
        ): Boolean = SettingsManager.getInstance(context).darkMode.isLightTheme(
            systemDark = isSystemInDarkMode(context),
            daylight = daylight
        )

        /**
         * Read the effective night configuration of the application, not the requested
         * UiModeManager.nightMode policy and not a per-Activity localNightMode override.
         * This also respects manual overrides of the system's scheduled dark theme.
         */
        private fun isSystemInDarkMode(context: Context): Boolean {
            val uiMode = context.applicationContext.resources.configuration.uiMode
            return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

    }
}
