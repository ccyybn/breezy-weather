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

package org.breezyweather.sources.qweather.json

import kotlinx.serialization.Serializable

@Serializable
data class QWeatherDaily(
    val fxDate: String?,
    val sunrise: String?,
    val sunset: String?,
    val moonrise: String?,
    val moonset: String?,
    val moonPhase: String?,
    val moonPhaseIcon: String?,
    val tempMax: String?,
    val tempMin: String?,
    val iconDay: String?,
    val textDay: String?,
    val iconNight: String?,
    val textNight: String?,
    val wind360Day: String?,
    val windDirDay: String?,
    val windScaleDay: String?,
    val windSpeedDay: String?,
    val wind360Night: String?,
    val windDirNight: String?,
    val windScaleNight: String?,
    val windSpeedNight: String?,
    val humidity: String?,
    val precip: String?,
    val pressure: String?,
    val vis: String?,
    val cloud: String?,
    val uvIndex: String?,
)
