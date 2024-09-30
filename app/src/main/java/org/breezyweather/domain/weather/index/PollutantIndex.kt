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

package org.breezyweather.domain.weather.index

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import org.breezyweather.R
import kotlin.math.roundToInt

enum class PollutantIndex(
    val id: String,
    val thresholds: List<Float>
) {
    O3("o3", listOf(0f, 106.01227f, 137.42331f, 166.87117f, 206.13496f, 392.63803f)),
    NO2("no2", listOf(0f, 99.73538f, 188.17996f, 677.4479f, 1221.288f, 2350.3677f)),
    PM10("pm10", listOf(0f, 54f, 154f, 254f, 354f, 424f)),
    PM25("pm25", listOf(0f, 12f, 35.4f, 55.4f, 150.4f, 250.4f)),
    SO2("so2", listOf(0f, 91.71575f, 196.53374f, 484.78323f, 796.61676f, 1582.7517f)),
    CO("co", listOf(0f, 5.040654f, 10.768671f, 14.205481f, 17.64229f, 34.82634f));

    companion object {
        // Plume 2023
        val aqiThresholds = listOf(0f, 50f, 100f, 150f, 200f, 300f)
        val namesArrayId = R.array.air_quality_levels
        val descriptionsArrayId = R.array.air_quality_level_descriptions
        val colorsArrayId = R.array.air_quality_level_colors

        val indexFreshAir = aqiThresholds[1]
        val indexHighPollution = aqiThresholds[3]
        val indexExcessivePollution = aqiThresholds.last()

        fun getAqiToLevel(aqi: Double?, thresholds: List<Float>?): Int? {
            if (aqi == null) return null
            val level = thresholds?.indexOfLast { aqi > it } ?: aqiThresholds.indexOfLast { aqi > it }
            return if (level >= 0) level else null
        }

        @ColorInt
        fun getAqiToColor(context: Context, aqi: Double?, thresholds: List<Float>? = null): Int {
            if (aqi == null) return Color.TRANSPARENT
            val level = getAqiToLevel(aqi, thresholds)
            return if (level != null) context.resources.getIntArray(colorsArrayId)
                .getOrNull(level) ?: Color.TRANSPARENT
            else Color.TRANSPARENT
        }

        fun getAqiToName(context: Context, aqi: Double?, thresholds: List<Float>? = null): String? {
            if (aqi == null) return null
            val level = getAqiToLevel(aqi, thresholds)
            return if (level != null) context.resources.getStringArray(namesArrayId).getOrNull(level) else null
        }

        fun getAqiToDescription(context: Context, aqi: Double?, thresholds: List<Float>? = null): String? {
            if (aqi == null) return null
            val level = getAqiToLevel(aqi, thresholds)
            return if (level != null) context.resources.getStringArray(descriptionsArrayId).getOrNull(level) else null
        }
    }

    private fun getIndex(cp: Double, bpLo: Float, bpHi: Float, inLo: Float, inHi: Float): Int {
        // Result will be incorrect if we don’t cast to double
        return ((inHi.toDouble() - inLo.toDouble()) / (bpHi.toDouble() - bpLo.toDouble()) * (cp - bpLo.toDouble()) + inLo.toDouble()).roundToInt()
    }

    private fun getIndex(cp: Double, level: Int): Int {
        return if (level < thresholds.lastIndex) {
            getIndex(
                cp,
                thresholds[level],
                thresholds[level + 1],
                aqiThresholds[level],
                aqiThresholds[level + 1]
            )
        } else {
            // Continue producing a linear index above lastIndex
            ((cp * aqiThresholds.last()) / thresholds.last()).roundToInt()
        }
    }

    fun getIndex(cp: Double?): Int? {
        if (cp == null || cp < 0) return null
        val level = if (cp > 0) thresholds.indexOfLast { cp > it } else 0
        return if (level >= 0) getIndex(cp, level) else null
    }

    fun getLevel(cp: Double?): Int? {
        if (cp == null || cp < 0) return null
        val level = if (cp > 0) thresholds.indexOfLast { cp > it } else 0
        return if (level >= 0) level else null
    }

    val excessivePollution = thresholds.last()

    fun getName(context: Context, cp: Double?): String? = getAqiToName(context, cp, thresholds)
    fun getDescription(context: Context, cp: Double?): String? = getAqiToDescription(context, cp, thresholds)

    @ColorInt
    fun getColor(context: Context, cp: Double?): Int = getAqiToColor(context, cp, thresholds)
}
