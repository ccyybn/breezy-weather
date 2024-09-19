/**
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

package org.breezyweather.theme.weatherView

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.WeatherCode
import org.breezyweather.BreezyWeather
import org.breezyweather.R
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.theme.weatherView.WeatherView.WeatherKindRule

object WeatherViewController {

    fun getWeatherCode(
        @WeatherKindRule weatherKind: Int
    ): WeatherCode = when (weatherKind) {
        WeatherView.WEATHER_KIND_CLOUDY -> WeatherCode.CLOUDY
        WeatherView.WEATHER_KIND_CLOUD -> WeatherCode.PARTLY_CLOUDY
        WeatherView.WEATHER_KIND_FOG -> WeatherCode.FOG
        WeatherView.WEATHER_KIND_HAIL -> WeatherCode.HAIL
        WeatherView.WEATHER_KIND_HAZE -> WeatherCode.HAZE
        WeatherView.WEATHER_KIND_RAINY -> WeatherCode.RAIN
        WeatherView.WEATHER_KIND_SLEET -> WeatherCode.SLEET
        WeatherView.WEATHER_KIND_SNOW -> WeatherCode.SNOW
        WeatherView.WEATHER_KIND_THUNDERSTORM -> WeatherCode.THUNDERSTORM
        WeatherView.WEATHER_KIND_THUNDER -> WeatherCode.THUNDER
        WeatherView.WEATHER_KIND_WIND -> WeatherCode.WIND
        else -> WeatherCode.CLEAR
    }

    @WeatherKindRule
    fun getWeatherKind(location: Location?): Int = getWeatherKind(
        when (location?.backgroundWeatherKind ?: "auto") {
            "auto" -> location?.weather?.current?.weatherCode
            else -> WeatherCode.getInstance(location!!.backgroundWeatherKind)
        }
    )

    fun isDaylight(location: Location?): Boolean = when (location?.backgroundDayNightType ?: "auto") {
        "day" -> true
        "night" -> false
        else -> location?.isDaylight ?: true
    }

    @WeatherKindRule
    fun getWeatherKind(weatherCode: WeatherCode?): Int = when (weatherCode) {
        WeatherCode.CLEAR -> WeatherView.WEATHER_KIND_CLEAR
        WeatherCode.PARTLY_CLOUDY -> WeatherView.WEATHER_KIND_CLOUD
        WeatherCode.CLOUDY -> WeatherView.WEATHER_KIND_CLOUDY
        WeatherCode.RAIN -> WeatherView.WEATHER_KIND_RAINY
        WeatherCode.LIGHT_RAIN -> WeatherView.WEATHER_KIND_LIGHT_RAIN
        WeatherCode.SHOWERY_RAIN, WeatherCode.MODERATE_RAIN -> WeatherView.WEATHER_KIND_MODERATE_RAIN
        WeatherCode.HEAVY_RAIN -> WeatherView.WEATHER_KIND_HEAVY_RAIN
        WeatherCode.RAINSTORM -> WeatherView.WEATHER_KIND_RAINSTORM
        WeatherCode.SNOW -> WeatherView.WEATHER_KIND_SNOW
        WeatherCode.LIGHT_SNOW -> WeatherView.WEATHER_KIND_LIGHT_SNOW
        WeatherCode.SHOWERY_SNOW, WeatherCode.MODERATE_SNOW -> WeatherView.WEATHER_KIND_MODERATE_SNOW
        WeatherCode.HEAVY_SNOW -> WeatherView.WEATHER_KIND_HEAVY_SNOW
        WeatherCode.SNOWSTORM -> WeatherView.WEATHER_KIND_SNOWSTORM
        WeatherCode.WIND -> WeatherView.WEATHER_KIND_WIND
        WeatherCode.FOG -> WeatherView.WEATHER_KIND_FOG
        WeatherCode.HAZE -> WeatherView.WEATHER_KIND_HAZE
        WeatherCode.SLEET -> WeatherView.WEATHER_KIND_SLEET
        WeatherCode.HAIL -> WeatherView.WEATHER_KIND_HAIL
        WeatherCode.THUNDER -> WeatherView.WEATHER_KIND_THUNDER
        WeatherCode.THUNDERSTORM -> WeatherView.WEATHER_KIND_THUNDERSTORM
        else -> WeatherView.WEATHER_KIND_CLEAR
    }

    fun getWeatherText(weatherCode: WeatherCode): String {
        val values = BreezyWeather.instance.resources.getStringArray(R.array.live_wallpaper_weather_kinds)
        val index = when (weatherCode) {
            WeatherCode.CLEAR -> 0
            WeatherCode.PARTLY_CLOUDY -> 1
            WeatherCode.CLOUDY -> 2
            WeatherCode.RAIN, WeatherCode.SHOWERY_RAIN -> 3
            WeatherCode.LIGHT_RAIN -> 4
            WeatherCode.MODERATE_RAIN -> 5
            WeatherCode.HEAVY_RAIN -> 6
            WeatherCode.RAINSTORM -> 7
            WeatherCode.SNOW, WeatherCode.SHOWERY_SNOW -> 8
            WeatherCode.LIGHT_SNOW -> 9
            WeatherCode.MODERATE_SNOW -> 10
            WeatherCode.HEAVY_SNOW -> 11
            WeatherCode.SNOWSTORM -> 12
            WeatherCode.SLEET -> 13
            WeatherCode.HAIL -> 14
            WeatherCode.FOG -> 15
            WeatherCode.HAZE -> 16
            WeatherCode.THUNDER -> 17
            WeatherCode.THUNDERSTORM -> 18
            WeatherCode.WIND -> 19
            else -> 0
        }
        // ignore "automatic"
        return values[index + 1]
    }
}
