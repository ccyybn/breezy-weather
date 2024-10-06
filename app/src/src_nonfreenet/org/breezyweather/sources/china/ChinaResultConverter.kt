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

package org.breezyweather.sources.china

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
import org.breezyweather.common.extensions.toDate
import org.breezyweather.common.extensions.toLocalDateTime
import org.breezyweather.sources.china.json.ChinaAqi
import org.breezyweather.sources.china.json.ChinaCurrent
import org.breezyweather.sources.china.json.ChinaForecastDaily
import org.breezyweather.sources.china.json.ChinaForecastHourly
import org.breezyweather.sources.china.json.ChinaForecastMinutely
import org.breezyweather.sources.china.json.ChinaForecastResult
import org.breezyweather.sources.china.json.ChinaHourlyWind
import org.breezyweather.sources.china.json.ChinaHourlyWindValue
import org.breezyweather.sources.china.json.ChinaLocationResult
import org.breezyweather.sources.china.json.ChinaValueListInt
import java.time.LocalTime
import java.util.Date
import java.util.Objects
import java.util.regex.Pattern

fun convert(
    location: Location?, // Null if location search, current location if reverse geocoding
    result: ChinaLocationResult
): Location {
    return (location ?: Location())
        .copy(
            cityId = result.locationKey!!.replace("weathercn:", ""),
            latitude = location?.latitude ?: result.latitude!!.toDouble(),
            longitude = location?.longitude ?: result.longitude!!.toDouble(),
            timeZone = "Asia/Shanghai",
            country = "",
            countryCode = "CN",
            admin2 = result.affiliation, // TODO: Double check if admin1 or admin2
            city = result.name ?: ""
        )
}

fun convert(
    location: Location,
    forecastResult: ChinaForecastResult
): WeatherWrapper {
    // If the API doesn’t return current, hourly or daily, consider data as garbage and keep cached data
    if (forecastResult.current == null || forecastResult.forecastDaily == null || forecastResult.forecastHourly == null) {
        throw InvalidOrIncompleteDataException()
    }

    return WeatherWrapper(
        current = getCurrent(forecastResult.current, forecastResult.aqi, forecastResult.minutely),
        dailyForecast = getDailyList(
            forecastResult.current.pubTime,
            location,
            forecastResult.forecastDaily
        ),
        hourlyForecast = getHourlyList(
            location,
            forecastResult.forecastHourly
        ),
        minutelyForecast = getMinutelyList(
            location,
            forecastResult.minutely
        ),
        alertList = getAlertList(forecastResult)
    )
}

fun getCurrent(
    current: ChinaCurrent?,
    aqi: ChinaAqi?,
    minutelyResult: ChinaForecastMinutely?
): Current? {
    if (current == null) return null

    return Current(
        weatherText = getWeatherText(current.weather),
        weatherCode = getWeatherCode(current.weather),
        temperature = Temperature(
            temperature = current.temperature?.value?.toDoubleOrNull(),
            apparentTemperature = current.feelsLike?.value?.toDoubleOrNull()
        ),
        wind = if (current.wind != null) Wind(
            degree = current.wind.direction?.value?.toDoubleOrNull(),
            speed = current.wind.speed?.value?.toDoubleOrNull()?.div(3.6)
        ) else null,
        uV = if (current.uvIndex != null) {
            UV(index = current.uvIndex.toDoubleOrNull())
        } else null,
        airQuality = aqi?.let {
            AirQuality(
                pM25 = it.pm25?.toDoubleOrNull(),
                pM10 = it.pm10?.toDoubleOrNull(),
                sO2 = it.so2?.toDoubleOrNull(),
                nO2 = it.no2?.toDoubleOrNull(),
                o3 = it.o3?.toDoubleOrNull(),
                cO = it.co?.toDoubleOrNull()
            )
        },
        relativeHumidity = if (!current.humidity?.value.isNullOrEmpty()) {
            current.humidity!!.value!!.toDoubleOrNull()
        } else null,
        pressure = if (!current.pressure?.value.isNullOrEmpty()) {
            current.pressure!!.value!!.toDoubleOrNull()
        } else null,
        visibility = if (!current.visibility?.value.isNullOrEmpty()) {
            current.visibility!!.value!!.toDoubleOrNull()?.times(1000)
        } else null,
        hourlyForecast = if (minutelyResult?.precipitation != null) {
            minutelyResult.precipitation.description
        } else null
    )
}

@SuppressLint("NewApi")
private fun getDailyList(
    publishDate: Date,
    location: Location,
    dailyForecast: ChinaForecastDaily
): List<Daily> {
    if (dailyForecast.weather == null || dailyForecast.weather.value.isNullOrEmpty()) return emptyList()
    val dailyList: MutableList<Daily> = ArrayList(dailyForecast.weather.value.size)
    val localDateTime = publishDate.toLocalDateTime(location.zoneId)

    dailyForecast.weather.value.forEachIndexed { index, weather ->
        val startOfDay = localDateTime.with(LocalTime.MIN).plusDays(index.toLong())
        val aqi = dailyForecast.aqi?.value?.getOrNull(index)
        dailyList.add(
            Daily(
                date = startOfDay.toDate(location.zoneId),
                day = HalfDay(
                    weatherText = getWeatherText(weather.from),
                    weatherPhase = getWeatherText(weather.from),
                    weatherCode = getWeatherCode(weather.from),
                    temperature = Temperature(
                        temperature = dailyForecast.temperature?.value?.getOrNull(index)?.from?.toDoubleOrNull()
                    ),
                    precipitationProbability = PrecipitationProbability(
                        total = getPrecipitationProbability(dailyForecast, index)
                    ),
                    wind = if (dailyForecast.wind != null) Wind(
                        degree = dailyForecast.wind.direction?.value?.getOrNull(index)?.from?.toDoubleOrNull(),
                        speed = dailyForecast.wind.speed?.value?.getOrNull(index)?.from?.toDoubleOrNull()?.div(3.6)
                    ) else null
                ),
                night = HalfDay(
                    weatherText = getWeatherText(weather.to),
                    weatherPhase = getWeatherText(weather.to),
                    weatherCode = getWeatherCode(weather.to),
                    temperature = Temperature(
                        temperature = dailyForecast.temperature?.value?.getOrNull(index)?.to?.toDoubleOrNull()
                    ),
                    precipitationProbability = PrecipitationProbability(
                        total = getPrecipitationProbability(dailyForecast, index)
                    ),
                    wind = if (dailyForecast.wind != null) Wind(
                        degree = dailyForecast.wind.direction?.value?.getOrNull(index)?.to?.toDoubleOrNull(),
                        speed = dailyForecast.wind.speed?.value?.getOrNull(index)?.to?.toDoubleOrNull()?.div(3.6)
                    ) else null
                ),
                airQuality = if (aqi != null) AirQuality(reverseIndex(aqi), null, null, null, null, null) else null,
//                sun = Astro(
//                    riseDate = dailyForecast.sunRiseSet?.value?.getOrNull(index)?.from,
//                    setDate = dailyForecast.sunRiseSet?.value?.getOrNull(index)?.to
//                )
            )
        )
    }
    return dailyList
}

private fun getPrecipitationProbability(forecast: ChinaForecastDaily, index: Int): Double? {
    if (forecast.precipitationProbability == null ||
        forecast.precipitationProbability.value.isNullOrEmpty()
    ) {
        return null
    }

    return forecast.precipitationProbability.value.getOrNull(index)?.toDoubleOrNull()
}

private val pm25Thresholds = listOf(0f, 35f, 75f, 115f, 150f, 250f, 350f, 500f)
private val aqiThresholds = listOf(0f, 50f, 100f, 150f, 200f, 300f, 400f, 500f)

private fun reverseIndex(aqi: Int, bpLo: Float, bpHi: Float, inLo: Float, inHi: Float): Double {
    return (aqi.toDouble() - inLo.toDouble()) * (bpHi.toDouble() - bpLo.toDouble()) / (inHi.toDouble() - inLo.toDouble()) + bpLo.toDouble()
}


private fun reverseIndex(aqi: Int?): Double? {
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

@SuppressLint("NewApi")
private fun getHourlyValueMap(
    location: Location,
    valueList: ChinaValueListInt?
): Map<Date, Int?> {
    return if (valueList?.pubTime != null && valueList.value != null) {
        valueList.value.mapIndexed { index, i -> Pair(index, i) }
            .groupBy {
                valueList.pubTime.toLocalDateTime(location.zoneId)
                    .withMinute(0).withSecond(0).withNano(0).plusHours(it.first.toLong())
                    .toDate(location.zoneId)
            }.mapValues { entry -> entry.value.map { it.second }.firstOrNull() }
    } else {
        HashMap()
    }
}

@SuppressLint("NewApi")
private fun getHourlyWindMap(
    windList: ChinaHourlyWind?
): Map<Date, ChinaHourlyWindValue?> {
    return if (windList?.value != null) {
        windList.value
            .filter { it.datetime != null }
            .groupBy {
                it.datetime!!
            }.mapValues { entry -> entry.value.firstOrNull() }
    } else {
        HashMap()
    }
}


@SuppressLint("NewApi")
private fun getHourlyList(
    location: Location,
    hourlyForecast: ChinaForecastHourly
): List<HourlyWrapper> {
    if (hourlyForecast.weather == null || hourlyForecast.weather.value.isNullOrEmpty() || hourlyForecast.weather.pubTime == null) return emptyList()

    val temperatures = getHourlyValueMap(location, hourlyForecast.temperature)
    val aqis = getHourlyValueMap(location, hourlyForecast.aqi)
    val winds = getHourlyWindMap(hourlyForecast.wind)

    val localDateTime = hourlyForecast.weather.pubTime.toLocalDateTime(location.zoneId)

    val hourlyList: MutableList<HourlyWrapper> = ArrayList(hourlyForecast.weather.value.size)
    hourlyForecast.weather.value.forEachIndexed { index, weather ->
        val date = localDateTime.withMinute(0).withSecond(0).withNano(0).plusHours(index.toLong()).toDate(location.zoneId)
        val aqi = aqis[date]
        hourlyList.add(
            HourlyWrapper(
                date = date,
                weatherText = getWeatherText(weather.toString()),
                weatherCode = getWeatherCode(weather.toString()),
                temperature = Temperature(
                    temperature = temperatures[date]?.toDouble()
                ),
                airQuality = if (aqi != null) AirQuality(reverseIndex(aqi), null, null, null, null, null) else null,
                wind = if (hourlyForecast.wind != null) Wind(
                    degree = winds[date]?.direction?.toDoubleOrNull(),
                    speed = winds[date]?.speed?.toDoubleOrNull()?.div(3.6)
                ) else null
            )
        )
    }
    return hourlyList
}

@SuppressLint("NewApi")
private fun getMinutelyList(
    location: Location,
    minutelyResult: ChinaForecastMinutely?
): List<Minutely> {
    if (minutelyResult?.precipitation == null || minutelyResult.precipitation.value.isNullOrEmpty()) return emptyList()
    val current = minutelyResult.precipitation.pubTime ?: return emptyList()
    val localDateTime = current.toLocalDateTime(location.zoneId)
    val minutelyList: MutableList<Minutely> = ArrayList(minutelyResult.precipitation.value.size)

    minutelyResult.precipitation.value.forEachIndexed { minute, precipitation ->
        val time = localDateTime.withSecond(0).withNano(0).plusMinutes(minute.toLong())
        minutelyList.add(
            Minutely(
                date = time.toDate(location.zoneId),
                minuteInterval = 1,
                precipitationIntensity = precipitation.times(60) // mm/min -> mm/h
            )
        )
    }
    return minutelyList
}

private fun getAlertList(result: ChinaForecastResult): List<Alert> {
    if (result.alerts.isNullOrEmpty()) return emptyList()

    return result.alerts.map { alert ->
        Alert(
            // Create unique ID from: title, level, start time
            alertId = Objects.hash(alert.title, alert.detail, alert.level, alert.pubTime?.time ?: System.currentTimeMillis()).toString(),
            startDate = alert.pubTime,
            headline = getHeadLine(alert.title, alert.detail),
            description = alert.detail,
            severity = getAlertSeverity(alert.level),
            color = getAlertColor(alert.level) ?: Alert.colorFromSeverity(AlertSeverity.UNKNOWN)
        )
    }.sortedWith(compareByDescending<Alert> { it.severity.id }.thenByDescending(Alert::startDate))
}

private fun getHeadLine(title: String?, description: String?): String? {
    if (description == null) return title
    var result: String? = null

    var pattern = Pattern.compile("""^(.+?)气象台.+?发布“?(.+?)预警""")
    var matcher = pattern.matcher(description)
    if (matcher.find()) result = "${matcher.group(1)!!}发布${matcher.group(2)!!}预警"

    if (result == null) {
        pattern = Pattern.compile("""^(.+?)气象台.+?更新“?(.+?)预警(信号)*为“?(.+?)预警""")
        matcher = pattern.matcher(description)
        if (matcher.find()) result = "${matcher.group(1)!!}更新${matcher.group(2)!!}预警为${matcher.group(4)!!}预警"
    }
    if (result == null) {
        pattern = Pattern.compile("""^(.+?)联合发布“?(.+?)预警""")
        matcher = pattern.matcher(description)
        if (matcher.find()) result = "${matcher.group(1)!!}联合发布${matcher.group(2)!!}预警"
    }
    if (result == null) {
        pattern = Pattern.compile("""^(.+?)(\d+年)*(\d+月)*(\d+日)*(\d+时)*(\d+分)*发布“?(.+?)预警""")
        matcher = pattern.matcher(description)
        if (matcher.find()) result = "${matcher.group(1)!!}发布${matcher.group(7)!!}预警"
    }
    if (result == null) {
        pattern = Pattern.compile("""^(.+?)(\d+年)*(\d+月)*(\d+日)*(\d+时)*(\d+分)*更新“?(.+?)预警(信号)*为“?(.+?)预警""")
        matcher = pattern.matcher(description)
        if (matcher.find()) result = "${matcher.group(1)!!}更新${matcher.group(7)!!}预警为${matcher.group(9)!!}预警"
    }
    return result ?: title
}

fun convertSecondary(
    location: Location,
    forecastResult: ChinaForecastResult
): SecondaryWeatherWrapper {
    if (forecastResult.current == null || forecastResult.forecastDaily == null || forecastResult.forecastHourly == null) {
        throw InvalidOrIncompleteDataException()
    }
    return SecondaryWeatherWrapper(
        current = getCurrent(forecastResult.current, forecastResult.aqi, forecastResult.minutely),
        precipitation = if (forecastResult.minutely?.precipitation != null) {
            forecastResult.minutely.precipitation.description
        } else null,
        airQuality = forecastResult.aqi?.let {
            AirQualityWrapper(
                current = AirQuality(
                    pM25 = it.pm25?.toDoubleOrNull(),
                    pM10 = it.pm10?.toDoubleOrNull(),
                    sO2 = it.so2?.toDoubleOrNull(),
                    nO2 = it.no2?.toDoubleOrNull(),
                    o3 = it.o3?.toDoubleOrNull(),
                    cO = it.co?.toDoubleOrNull()
                )
            )
        },
        minutelyForecast = getMinutelyList(location, forecastResult.minutely),
        alertList = getAlertList(forecastResult)
    )
}

private fun getWeatherText(icon: String?): String {
    return if (icon.isNullOrEmpty()) {
        "Unknown"
    } else when (icon) {
        "0", "00" -> "Clear"
        "1", "01" -> "Cloudy"
        "2", "02" -> "Overcast"
        "3", "03" -> "Shower"
        "4", "04" -> "Thundershower"
        "5", "05" -> "Hail"
        "6", "06" -> "Sleet"
        "7", "07" -> "Light Rain"
        "8", "08" -> "Moderate Rain"
        "9", "09" -> "Heavy Rain"
        "10" -> "Rainstorm"
        "11" -> "Heavy Rainstorm"
        "12" -> "Severe Rainstorm"
        "13" -> "Snow Flurry"
        "14" -> "Light Snow"
        "15" -> "Moderate Snow"
        "16" -> "Heavy Snow"
        "17" -> "Snowstorm"
        "18" -> "Fog"
        "19" -> "Freezing Rain"
        "20" -> "Sandstorm"
        "21" -> "Light to Moderate Rain"
        "22" -> "Moderate to Heavy Rain"
        "23" -> "Heavy Rain to Rainstorm"
        "24" -> "Rainstorm to Heavy Rainstorm"
        "25" -> "Heavy to Severe Rainstorm"
        "26" -> "Light to Moderate Snow"
        "27" -> "Moderate to Heavy Snow"
        "28" -> "Heavy Snow to Snowstorm"
        "29" -> "Dust"
        "30" -> "Sand"
        "31" -> "Severe Sandstorm"
        "53", "54", "55", "56" -> "Haze"
        else -> "Unknown"
    }
}

private fun getWeatherCode(icon: String?): WeatherCode? {
    return if (icon.isNullOrEmpty()) {
        null
    } else when (icon) {
        "0", "00" -> WeatherCode.CLEAR
        "1", "01" -> WeatherCode.PARTLY_CLOUDY
        "3", "03" -> WeatherCode.SHOWERY_RAIN
        "7", "07" -> WeatherCode.LIGHT_RAIN
        "8", "08", "21" -> WeatherCode.MODERATE_RAIN
        "9", "09", "22" -> WeatherCode.HEAVY_RAIN
        "10", "11", "12", "23", "24", "25" -> WeatherCode.RAINSTORM
        "4", "04" -> WeatherCode.THUNDERSTORM
        "5", "05" -> WeatherCode.HAIL
        "6", "06", "19" -> WeatherCode.SLEET
        "13" -> WeatherCode.SHOWERY_SNOW
        "14" -> WeatherCode.LIGHT_SNOW
        "15", "26" -> WeatherCode.MODERATE_SNOW
        "16", "27" -> WeatherCode.HEAVY_SNOW
        "17", "28" -> WeatherCode.SNOWSTORM
        "18", "32", "49", "57" -> WeatherCode.FOG
        "20", "29", "30" -> WeatherCode.WIND
        "53", "54", "55", "56" -> WeatherCode.HAZE
        else -> WeatherCode.CLOUDY
    }
}

private fun getAlertSeverity(color: String?): AlertSeverity {
    if (color.isNullOrEmpty()) return AlertSeverity.UNKNOWN
    return when (color) {
        "蓝", "蓝色" -> AlertSeverity.MINOR
        "黄", "黄色" -> AlertSeverity.MODERATE
        "橙", "橙色", "橘", "橘色", "橘黄", "橘黄色" -> AlertSeverity.SEVERE
        "红", "红色" -> AlertSeverity.EXTREME
        else -> AlertSeverity.UNKNOWN
    }
}

@ColorInt
private fun getAlertColor(color: String?): Int? {
    if (color.isNullOrEmpty()) return null
    return when (color) {
        "蓝", "蓝色" -> Color.rgb(66, 151, 231)
        "黄", "黄色" -> Color.rgb(255, 242, 184)
        "橙", "橙色", "橘", "橘色", "橘黄", "橘黄色" -> Color.rgb(255, 145, 0)
        "红", "红色" -> Color.rgb(255, 86, 86)
        else -> null
    }
}
