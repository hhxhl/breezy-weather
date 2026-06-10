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

package org.breezyweather.common.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIconUtils {

    private const val LAUNCHER_ALIAS_SUFFIX = ".LauncherAlias"

    fun setLauncherIconVisible(context: Context, visible: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            getLauncherAliasComponent(context),
            if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP
        )
    }

    fun isLauncherIconVisible(context: Context): Boolean {
        return context.packageManager.getComponentEnabledSetting(
            getLauncherAliasComponent(context)
        ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun getLauncherAliasComponent(context: Context): ComponentName {
        return ComponentName(context.packageName, context.packageName + LAUNCHER_ALIAS_SUFFIX)
    }
}
