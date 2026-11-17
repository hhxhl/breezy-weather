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
import android.widget.RemoteViews
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import org.breezyweather.domain.settings.ConfigStore
import org.breezyweather.background.receiver.widget.WidgetSplitForecastProvider
import org.breezyweather.common.source.PollenIndexSource

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
    ): RemoteViews = buildSplitForecastViews(
        context, location, cardStyle, cardAlpha, textColor, textSize,
        hideSubtitle, subtitleData, pollenIndexSource
    )
}
