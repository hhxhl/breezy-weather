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

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DarkModeTest {

    @Test
    fun `new dark modes are mutually exclusive`() {
        DarkMode.getInstance("system") shouldBe DarkMode.SYSTEM
        DarkMode.getInstance("day_night") shouldBe DarkMode.DAY_NIGHT
        DarkMode.getInstance("light") shouldBe DarkMode.LIGHT
        DarkMode.getInstance("dark") shouldBe DarkMode.DARK
    }

    @Test
    fun `legacy location override migrates to independent mode`() {
        DarkMode.fromLegacy("system", true) shouldBe DarkMode.DAY_NIGHT
        DarkMode.fromLegacy("dark", true) shouldBe DarkMode.DAY_NIGHT
        DarkMode.fromLegacy("light", true) shouldBe DarkMode.LIGHT
        DarkMode.fromLegacy("system", false) shouldBe DarkMode.SYSTEM
        DarkMode.fromLegacy("dark", false) shouldBe DarkMode.DARK
        DarkMode.fromLegacy("light", false) shouldBe DarkMode.LIGHT
    }

    @Test
    fun `theme modes do not override one another`() {
        DarkMode.SYSTEM.isLightTheme(systemDark = false, daylight = false) shouldBe true
        DarkMode.SYSTEM.isLightTheme(systemDark = true, daylight = true) shouldBe false
        DarkMode.DAY_NIGHT.isLightTheme(systemDark = true, daylight = true) shouldBe true
        DarkMode.DAY_NIGHT.isLightTheme(systemDark = false, daylight = false) shouldBe false
        DarkMode.DAY_NIGHT.isLightTheme(systemDark = true, daylight = null) shouldBe false
        DarkMode.DAY_NIGHT.isLightTheme(systemDark = false, daylight = null) shouldBe true
        DarkMode.LIGHT.isLightTheme(systemDark = true, daylight = false) shouldBe true
        DarkMode.DARK.isLightTheme(systemDark = false, daylight = true) shouldBe false
    }
}
