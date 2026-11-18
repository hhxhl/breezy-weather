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

package org.breezyweather.common.options

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.breezyweather.R
import org.breezyweather.common.utils.UnitUtils

enum class DarkMode(
    override val id: String,
    val value: Int,
) : BaseEnum {

    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DAY_NIGHT("day_night", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    ;

    companion object {

        fun getInstance(
            value: String,
        ) = entries.firstOrNull {
            it.id == value
        } ?: SYSTEM

        /** Translate the former two-setting combination into one unambiguous mode. */
        fun fromLegacy(mode: String?, followLocationDayNight: Boolean): DarkMode {
            val selected = getInstance(mode ?: SYSTEM.id)
            return if (followLocationDayNight && selected != LIGHT) DAY_NIGHT else selected
        }
    }

    /** Resolve the preferred appearance without coupling it to Android resources or activities. */
    fun isLightTheme(systemDark: Boolean, daylight: Boolean?): Boolean = when (this) {
        SYSTEM -> !systemDark
        DAY_NIGHT -> daylight ?: !systemDark
        LIGHT -> true
        DARK -> false
    }

    override val valueArrayId = R.array.dark_mode_values
    override val nameArrayId = R.array.dark_modes

    override fun getName(context: Context) = UnitUtils.getName(context, this)
}
