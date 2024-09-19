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

package org.breezyweather.sources.qweather

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.annotation.ColorInt
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.AirQuality
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.AlertSeverity
import breezyweather.domain.weather.model.Current
import breezyweather.domain.weather.model.Daily
import breezyweather.domain.weather.model.HalfDay
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.PrecipitationProbability
import breezyweather.domain.weather.model.Temperature
import breezyweather.domain.weather.model.UV
import breezyweather.domain.weather.model.WeatherCode
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.wrappers.AirQualityWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.SecondaryWeatherWrapper
import breezyweather.domain.weather.wrappers.WeatherWrapper
import org.breezyweather.common.exceptions.InvalidOrIncompleteDataException
import org.breezyweather.common.exceptions.SecondaryWeatherException
import org.breezyweather.common.extensions.toDate
import org.breezyweather.common.extensions.toLocalDateTime
import org.breezyweather.sources.qweather.json.QWeatherAirDailyResult
import org.breezyweather.sources.qweather.json.QWeatherAirResult
import org.breezyweather.sources.qweather.json.QWeatherDailyResult
import org.breezyweather.sources.qweather.json.QWeatherHourlyResult
import org.breezyweather.sources.qweather.json.QWeatherLocation
import org.breezyweather.sources.qweather.json.QWeatherMinuteResult
import org.breezyweather.sources.qweather.json.QWeatherNowResult
import org.breezyweather.sources.qweather.json.QWeatherWarningResult
import java.text.SimpleDateFormat
import java.util.Locale

fun convertLocation(
    location: Location?, // Null if location search, current location if reverse geocoding
    result: QWeatherLocation
): Location {
    return (location ?: Location())
        .copy(
            cityId = result.id,
            latitude = location?.latitude ?: result.lat!!.toDouble(),
            longitude = location?.longitude ?: result.lon!!.toDouble(),
            timeZone = result.tz!!,
            country = result.country!!,
            countryCode = "CN",
            admin1 = result.adm1,
            admin2 = result.adm2,
            city = result.name!!,
            weatherSource = "qweather"
        )
}

fun convert(
    location: Location,
    dailyResult: QWeatherDailyResult,
    hourlyResult: QWeatherHourlyResult,
    airDailyResult: QWeatherAirDailyResult,
    nowResult: QWeatherNowResult,
    airResult: QWeatherAirResult,
    warningResult: QWeatherWarningResult,
    minutelyResult: QWeatherMinuteResult,
    lang: String
): WeatherWrapper {
    if (dailyResult.code != null && dailyResult.code != "200" ||
        hourlyResult.code != null && hourlyResult.code != "200" ||
        nowResult.code != null && nowResult.code != "200"
    ) {
        throw InvalidOrIncompleteDataException()
    }
    return WeatherWrapper(
        current = Current(
            hourlyForecast = rectifySummary(minutelyResult.summary),
            weatherText = getWeatherText(nowResult.now?.icon, nowResult.now?.text, lang),
            weatherCode = getWeatherCode(nowResult.now?.icon),
            temperature = Temperature(
                temperature = nowResult.now?.temp?.toDouble(),
                realFeelTemperature = nowResult.now?.feelsLike?.toDouble()
            ),
            relativeHumidity = nowResult.now?.humidity?.toDouble(),
            pressure = nowResult.now?.pressure?.toDouble(),
            wind = Wind(
                degree = nowResult.now?.wind360?.toDouble(),
                speed = nowResult.now?.windSpeed?.toDouble()?.div(3.6)
            ),
            visibility = nowResult.now?.vis?.toDouble()?.times(1000),
            dewPoint = nowResult.now?.dew?.toDouble(),
            cloudCover = nowResult.now?.cloud?.toInt(),
            airQuality = airResult.now?.let {
                AirQuality(
                    pM25 = it.pm2p5?.toDoubleOrNull(),
                    pM10 = it.pm10?.toDoubleOrNull(),
                    sO2 = it.so2?.toDoubleOrNull(),
                    nO2 = it.no2?.toDoubleOrNull(),
                    o3 = it.o3?.toDoubleOrNull(),
                    cO = it.co?.toDoubleOrNull()
                )
            }
        ),
        alertList = getWarningList(warningResult),
        minutelyForecast = getMinutelyList(minutelyResult),
        dailyForecast = getDailyList(location, dailyResult, hourlyResult, airDailyResult, lang),
        hourlyForecast = getHourlyList(hourlyResult, lang)
    )
}


@SuppressLint("NewApi")
private fun getDailyList(
    location: Location,
    dailyResult: QWeatherDailyResult,
    hourlyResult: QWeatherHourlyResult,
    airDailyResult: QWeatherAirDailyResult,
    lang: String
): List<Daily> {
    if (dailyResult.daily.isNullOrEmpty()) return emptyList()
    val dailyList: MutableList<Daily> = ArrayList(dailyResult.daily.size)
    val dailyAirMap: MutableMap<String, String> = HashMap()
    airDailyResult.daily?.forEach { value ->
        dailyAirMap[value.fxDate!!] = value.aqi!!
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    sdf.timeZone = location.javaTimeZone

    dailyResult.daily.forEachIndexed { index, daily ->
        val date = sdf.parse(daily.fxDate!!)!!
        val startOfDay = date.toLocalDateTime(location.zoneId).plusHours(8).toDate(location.zoneId)
        val middleOfDay = date.toLocalDateTime(location.zoneId).plusHours(20).toDate(location.zoneId)
        val endOfDay = date.toLocalDateTime(location.zoneId).plusHours(32).toDate(location.zoneId)

        var precipitationProbabilityDay = hourlyResult.hourly?.asSequence()?.filter { it.fxTime!!.time >= startOfDay.time && it.fxTime.time < middleOfDay.time }
            ?.map { if (it.pop != null) it.pop.toDouble() / 100 else 0.0 }?.maxOrNull()

        var precipitationProbabilityNight = hourlyResult.hourly?.asSequence()?.filter { it.fxTime!!.time >= middleOfDay.time && it.fxTime.time < endOfDay.time }
            ?.map { if (it.pop != null) it.pop.toDouble() / 100 else 0.0 }?.maxOrNull()

        if (precipitationProbabilityDay?.isNaN() == true) precipitationProbabilityDay = null
        if (precipitationProbabilityNight?.isNaN() == true) precipitationProbabilityNight = null
        dailyList.add(
            Daily(
                date = date,
                day = HalfDay(
                    weatherText = getWeatherText(daily.iconDay, daily.textDay, lang),
                    weatherPhase = getWeatherText(daily.iconDay, daily.textDay, lang),
                    weatherCode = getWeatherCode(daily.iconDay),
                    temperature = Temperature(
                        temperature = daily.tempMax?.toDouble()
                    ),
                    wind = Wind(
                        degree = daily.wind360Day?.toDouble(),
                        speed = daily.windSpeedDay?.toDouble()?.div(3.6)
                    ),
                    precipitationProbability = if (precipitationProbabilityDay != null) PrecipitationProbability(
                        total = precipitationProbabilityDay.times(100)
                    ) else PrecipitationProbability(0.0)
                ),
                night = HalfDay(
                    weatherText = getWeatherText(daily.iconNight, daily.textNight, lang),
                    weatherPhase = getWeatherText(daily.iconNight, daily.textNight, lang),
                    weatherCode = getWeatherCode(daily.iconNight),
                    temperature = Temperature(
                        temperature = daily.tempMin?.toDouble()
                    ),
                    wind = Wind(
                        degree = daily.wind360Night?.toDouble(),
                        speed = daily.windSpeedNight?.toDouble()?.div(3.6)
                    ),
                    precipitationProbability = if (precipitationProbabilityNight != null) PrecipitationProbability(
                        total = precipitationProbabilityNight.times(100)
                    ) else PrecipitationProbability(0.0)
                ),
                airQuality = if (dailyAirMap.containsKey(daily.fxDate)) AirQuality(reverseIndex(dailyAirMap[daily.fxDate]?.toInt()), null, null, null, null, null) else null,
                uV = UV(index = daily.uvIndex?.toDouble()),
//                moonPhase = MoonPhase(angle = MoonPhase.getAngleFromEnglishDescription(daily.moonPhase))
            )
        )
    }
    return dailyList
}

private fun getHourlyList(
    hourlyResult: QWeatherHourlyResult,
    lang: String
): List<HourlyWrapper> {
    if (hourlyResult.hourly.isNullOrEmpty()) return emptyList()
    val hourlyList: MutableList<HourlyWrapper> = ArrayList(hourlyResult.hourly.size)
    hourlyResult.hourly.forEach {
        hourlyList.add(
            HourlyWrapper(
                date = it.fxTime!!,
                weatherText = getWeatherText(it.icon, it.text, lang),
                weatherCode = getWeatherCode(it.icon),
                temperature = Temperature(
                    temperature = it.temp?.toDouble()
                ),
                wind = Wind(
                    degree = it.wind360?.toDouble(),
                    speed = it.windSpeed?.toDouble()?.div(3.6)
                ),
                relativeHumidity = it.humidity?.toDouble(),
                precipitationProbability = PrecipitationProbability(total = it.pop?.toDouble()),
                precipitation = Precipitation(total = it.precip?.toDouble()),
                pressure = it.pressure?.toDouble(),
                cloudCover = it.cloud?.toInt(),
                dewPoint = it.dew?.toDouble()
            )
        )
    }
    return hourlyList
}

fun convertSecondary(
    nowResult: QWeatherNowResult,
    airResult: QWeatherAirResult,
    warningResult: QWeatherWarningResult,
    minutelyResult: QWeatherMinuteResult,
    lang: String
): SecondaryWeatherWrapper {
    if (nowResult.code != null && nowResult.code != "200" ||
        airResult.code != null && airResult.code != "200" ||
        warningResult.code != null && warningResult.code != "200" ||
        minutelyResult.code != null && minutelyResult.code != "200"
    ) {
        throw SecondaryWeatherException()
    }
    return SecondaryWeatherWrapper(
        current = Current(
            hourlyForecast = rectifySummary(minutelyResult.summary),
            weatherText = getWeatherText(nowResult.now?.icon, nowResult.now?.text, lang),
            weatherCode = getWeatherCode(nowResult.now?.icon),
            temperature = Temperature(
                temperature = nowResult.now?.temp?.toDouble(),
                realFeelTemperature = nowResult.now?.feelsLike?.toDouble()
            ),
            relativeHumidity = nowResult.now?.humidity?.toDouble(),
            pressure = nowResult.now?.pressure?.toDouble(),
            wind = Wind(
                degree = nowResult.now?.wind360?.toDouble(),
                speed = nowResult.now?.windSpeed?.toDouble()?.div(3.6)
            ),
            visibility = nowResult.now?.vis?.toDouble()?.times(1000),
            dewPoint = nowResult.now?.dew?.toDouble(),
            cloudCover = nowResult.now?.cloud?.toInt()
        ),
        precipitation = rectifySummary(minutelyResult.summary),
        airQuality = airResult.now?.let {
            AirQualityWrapper(
                current = AirQuality(
                    pM25 = it.pm2p5?.toDoubleOrNull(),
                    pM10 = it.pm10?.toDoubleOrNull(),
                    sO2 = it.so2?.toDoubleOrNull(),
                    nO2 = it.no2?.toDoubleOrNull(),
                    o3 = it.o3?.toDoubleOrNull(),
                    cO = it.co?.toDoubleOrNull()
                )
            )
        },
        alertList = getWarningList(warningResult),
        minutelyForecast = getMinutelyList(minutelyResult),
    )
}

private fun getMinutelyList(
    minutelyResult: QWeatherMinuteResult
): List<Minutely> {
    if (minutelyResult.minutely.isNullOrEmpty()) return emptyList()
    val minutelyList: MutableList<Minutely> = ArrayList(minutelyResult.minutely.size)

    minutelyResult.minutely.forEach {
        minutelyList.add(
            Minutely(
                date = it.fxTime,
                minuteInterval = 5,
                precipitationIntensity = it.precip?.toDouble()?.times(12) // mm/min -> mm/h
            )
        )
    }
    return minutelyList
}

private fun getWarningList(result: QWeatherWarningResult): List<Alert> {
    if (result.warning.isNullOrEmpty()) return emptyList()
    return result.warning.map {
        Alert(
            alertId = it.id ?: System.currentTimeMillis().toString(),
            startDate = it.pubTime,
            headline = it.title,
            description = it.text,
            severity = getWarningSeverity(it.severity),
            color = getWarningColor(it.severityColor) ?: Alert.colorFromSeverity(AlertSeverity.UNKNOWN)
        )
    }.sortedWith(compareByDescending<Alert> { it.severity.id }.thenByDescending(Alert::startDate))
}

private fun getWarningSeverity(severity: String?): AlertSeverity {
    if (severity.isNullOrEmpty()) return AlertSeverity.UNKNOWN
    return when (severity) {
        "Cancel", "None", "Unknown", "Standard", "Minor" -> AlertSeverity.MINOR
        "Moderate" -> AlertSeverity.MODERATE
        "Major", "Severe" -> AlertSeverity.SEVERE
        "Extreme" -> AlertSeverity.EXTREME
        else -> AlertSeverity.UNKNOWN
    }
}

@ColorInt
private fun getWarningColor(severityColor: String?): Int? {
    if (severityColor.isNullOrEmpty()) return null
    return when (severityColor) {
        "White" -> Color.rgb(200, 200, 200)
        "Blue" -> Color.rgb(66, 151, 231)
        "Yellow" -> Color.rgb(255, 242, 184)
        "Orange" -> Color.rgb(255, 145, 0)
        "Red" -> Color.rgb(255, 86, 86)
        "Black" -> Color.rgb(0, 0, 0)
        else -> null
    }
}

fun getWeatherText(icon: String?, text: String?, lang: String): String? {
    return if (icon.isNullOrEmpty()) {
        null
    } else if (lang == "en" || text.isNullOrEmpty()) when (icon) {
        "100" -> "Sunny"
        "101" -> "Cloudy"
        "102" -> "Few Clouds"
        "103" -> "Partly Cloudy"
        "104" -> "Overcast"
        "150" -> "Clear"
        "151" -> "Cloudy"
        "152" -> "Few Clouds"
        "153" -> "Partly Cloudy"
        "300" -> "Shower"
        "301" -> "Heavy Shower"
        "302" -> "Thundershower"
        "303" -> "Heavy Thunderstorm"
        "304" -> "Hail"
        "305" -> "Light Rain"
        "306" -> "Moderate Rain"
        "307" -> "Heavy Rain"
        "308" -> "Extreme Rain"
        "309" -> "Drizzle Rain"
        "310" -> "Rainstorm"
        "311" -> "Heavy Rainstorm"
        "312" -> "Severe Rainstorm"
        "313" -> "Freezing Rain"
        "314" -> "Light to Moderate Rain"
        "315" -> "Moderate to Heavy Rain"
        "316" -> "Heavy Rain to Rainstorm"
        "317" -> "Rainstorm to Heavy Rainstorm"
        "318" -> "Heavy to Severe Rainstorm"
        "350" -> "Shower"
        "351" -> "Heavy Shower"
        "399" -> "Rain"
        "400" -> "Light Snow"
        "401" -> "Moderate Snow"
        "402" -> "Heavy Snow"
        "403" -> "Snowstorm"
        "404" -> "Sleet"
        "405" -> "Rain and Snow"
        "406" -> "Shower Rain and Snow"
        "407" -> "Snow Flurry"
        "408" -> "Light to Moderate Snow"
        "409" -> "Moderate to Heavy Snow"
        "410" -> "Heavy Snow to Snowstorm"
        "456" -> "Shower Rain and Snow"
        "457" -> "Snow Flurry"
        "499" -> "Snow"
        "500" -> "Mist"
        "501" -> "Fog"
        "502" -> "Haze"
        "503" -> "Sand"
        "504" -> "Dust"
        "507" -> "Sandstorm"
        "508" -> "Severe Sandstorm"
        "509" -> "Dense Fog"
        "510" -> "Strong Fog"
        "511" -> "Moderate Haze"
        "512" -> "Heavy Haze"
        "513" -> "Severe Haze"
        "514" -> "Heavy Fog"
        "515" -> "Extra Heavy Fog"
        "900" -> "Hot"
        "901" -> "Cold"
        "999" -> "Unknown"
        else -> "Unknown"
    } else text
}

fun getWeatherCode(icon: String?): WeatherCode? {
    return if (icon.isNullOrEmpty()) {
        null
    } else when (icon) {
        "100" -> WeatherCode.CLEAR
        "101" -> WeatherCode.PARTLY_CLOUDY
        "102" -> WeatherCode.PARTLY_CLOUDY
        "103" -> WeatherCode.PARTLY_CLOUDY
        "104" -> WeatherCode.CLOUDY
        "150" -> WeatherCode.CLEAR
        "151" -> WeatherCode.PARTLY_CLOUDY
        "152" -> WeatherCode.PARTLY_CLOUDY
        "153" -> WeatherCode.PARTLY_CLOUDY
        "300" -> WeatherCode.SHOWERY_RAIN
        "301" -> WeatherCode.SHOWERY_RAIN
        "302" -> WeatherCode.THUNDERSTORM
        "303" -> WeatherCode.THUNDERSTORM
        "304" -> WeatherCode.HAIL
        "305" -> WeatherCode.LIGHT_RAIN
        "306" -> WeatherCode.MODERATE_RAIN
        "307" -> WeatherCode.HEAVY_RAIN
        "308" -> WeatherCode.RAINSTORM
        "309" -> WeatherCode.LIGHT_RAIN
        "310" -> WeatherCode.RAINSTORM
        "311" -> WeatherCode.RAINSTORM
        "312" -> WeatherCode.RAINSTORM
        "313" -> WeatherCode.SLEET
        "314" -> WeatherCode.MODERATE_RAIN
        "315" -> WeatherCode.HEAVY_RAIN
        "316" -> WeatherCode.RAINSTORM
        "317" -> WeatherCode.RAINSTORM
        "318" -> WeatherCode.RAINSTORM
        "350" -> WeatherCode.SHOWERY_RAIN
        "351" -> WeatherCode.SHOWERY_RAIN
        "399" -> WeatherCode.MODERATE_RAIN
        "400" -> WeatherCode.LIGHT_SNOW
        "401" -> WeatherCode.MODERATE_SNOW
        "402" -> WeatherCode.HEAVY_SNOW
        "403" -> WeatherCode.SNOWSTORM
        "404" -> WeatherCode.SLEET
        "405" -> WeatherCode.SLEET
        "406" -> WeatherCode.SLEET
        "407" -> WeatherCode.SHOWERY_SNOW
        "408" -> WeatherCode.MODERATE_SNOW
        "409" -> WeatherCode.HEAVY_SNOW
        "410" -> WeatherCode.SNOWSTORM
        "456" -> WeatherCode.SLEET
        "457" -> WeatherCode.SHOWERY_SNOW
        "499" -> WeatherCode.MODERATE_SNOW
        "500" -> WeatherCode.FOG
        "501" -> WeatherCode.FOG
        "502" -> WeatherCode.HAZE
        "503" -> WeatherCode.WIND
        "504" -> WeatherCode.WIND
        "507" -> WeatherCode.WIND
        "508" -> WeatherCode.WIND
        "509" -> WeatherCode.FOG
        "510" -> WeatherCode.FOG
        "511" -> WeatherCode.HAZE
        "512" -> WeatherCode.HAZE
        "513" -> WeatherCode.HAZE
        "514" -> WeatherCode.FOG
        "515" -> WeatherCode.FOG
        "900" -> WeatherCode.CLEAR
        "901" -> WeatherCode.CLOUDY
        "999" -> WeatherCode.CLOUDY
        else -> WeatherCode.CLOUDY
    }
}


private val pm25Thresholds = listOf(0f, 35f, 75f, 115f, 150f, 250f, 350f, 500f)
private val aqiThresholds = listOf(0f, 50f, 100f, 150f, 200f, 300f, 400f, 500f)

private fun reverseIndex(aqi: Int, bpLo: Float, bpHi: Float, inLo: Float, inHi: Float): Double {
    return (aqi.toDouble() - inLo.toDouble()) * (bpHi.toDouble() - bpLo.toDouble()) / (inHi.toDouble() - inLo.toDouble()) + bpLo.toDouble()
}


fun reverseIndex(aqi: Int?): Double? {
    if (aqi == null || aqi <= 0) return null
    val level = aqiThresholds.indexOfLast { aqi > it }
    return if (level < aqiThresholds.lastIndex) {
        reverseIndex(
            aqi,
            pm25Thresholds[level],
            pm25Thresholds[level + 1],
            aqiThresholds[level],
            aqiThresholds[level + 1]
        )
    } else {
        return aqi.toDouble() * pm25Thresholds.last().toDouble() / aqiThresholds.last().toDouble()
    }
}

fun rectifySummary(summary: String?): String? {
    return summary
        ?.replace("precip", "precipitation")
        ?.replace("Rain/Snow", "Precipitation")
}
