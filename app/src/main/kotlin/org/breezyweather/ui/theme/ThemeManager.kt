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
import org.breezyweather.common.options.DarkMode
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

        fun isLightTheme(
            context: Context,
            location: Location?,
        ) = isLightTheme(
            context = context,
            daylight = location?.isDaylight
        )

        /**
         * Resolve the theme for a location using the app preference.
         *
         * When location-based day/night is enabled, the location may override the base
         * dark theme. Otherwise, follow the selected app mode (including the actual
         * system theme for SYSTEM). A fixed light theme is always light.
         */
        fun isLightTheme(
            context: Context,
            daylight: Boolean?,
        ): Boolean {
            val settings = SettingsManager.getInstance(context)
            return when (settings.darkMode) {
                DarkMode.LIGHT -> true
                DarkMode.DARK -> if (settings.dayNightModeForLocations && daylight != null) {
                    daylight
                } else {
                    false
                }
                DarkMode.SYSTEM -> if (settings.dayNightModeForLocations && daylight != null) {
                    daylight
                } else {
                    !isSystemInDarkMode(context)
                }
            }
        }

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
