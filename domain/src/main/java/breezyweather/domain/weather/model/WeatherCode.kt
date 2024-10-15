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

package breezyweather.domain.weather.model

enum class WeatherCode(val id: String) {

    CLEAR("clear"),
    PARTLY_CLOUDY("partly_cloudy"),
    CLOUDY("cloudy"),
    RAIN("rain"),
    SHOWERY_RAIN("showery_rain"),
    LIGHT_RAIN("light_rain"),
    MODERATE_RAIN("moderate_rain"),
    HEAVY_RAIN("heavy_rain"),
    RAINSTORM("rainstorm"),
    SNOW("snow"),
    SHOWERY_SNOW("showery_snow"),
    LIGHT_SNOW("light_snow"),
    MODERATE_SNOW("moderate_snow"),
    HEAVY_SNOW("heavy_snow"),
    SNOWSTORM("snowstorm"),
    WIND("wind"),
    FOG("fog"),
    HAZE("haze"),
    SLEET("sleet"),
    HAIL("hail"),
    THUNDER("thunder"),
    THUNDERSTORM("thunderstorm");

    companion object {
        fun getInstance(
            value: String?
        ): WeatherCode? = WeatherCode.entries.firstOrNull {
            it.id.equals(value, ignoreCase = true)
        }
    }
}
