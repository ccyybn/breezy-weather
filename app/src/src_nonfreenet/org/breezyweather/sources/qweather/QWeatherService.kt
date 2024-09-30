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

import android.content.Context
import android.graphics.Color
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.wrappers.SecondaryWeatherWrapper
import breezyweather.domain.weather.wrappers.WeatherWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.BuildConfig
import org.breezyweather.R
import org.breezyweather.common.exceptions.ApiKeyMissingException
import org.breezyweather.common.exceptions.InvalidLocationException
import org.breezyweather.common.extensions.code
import org.breezyweather.common.extensions.codeWithCountry
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.preference.EditTextPreference
import org.breezyweather.common.preference.Preference
import org.breezyweather.common.source.ConfigurableSource
import org.breezyweather.common.source.HttpSource
import org.breezyweather.common.source.LocationSearchSource
import org.breezyweather.common.source.MainWeatherSource
import org.breezyweather.common.source.ReverseGeocodingSource
import org.breezyweather.common.source.SecondaryWeatherSource
import org.breezyweather.common.source.SecondaryWeatherSourceFeature
import org.breezyweather.settings.SourceConfigStore
import org.breezyweather.sources.qweather.json.QWeatherAirDailyResult
import org.breezyweather.sources.qweather.json.QWeatherAirResult
import org.breezyweather.sources.qweather.json.QWeatherDailyResult
import org.breezyweather.sources.qweather.json.QWeatherHourlyResult
import org.breezyweather.sources.qweather.json.QWeatherLocation
import org.breezyweather.sources.qweather.json.QWeatherMinuteResult
import org.breezyweather.sources.qweather.json.QWeatherNowResult
import org.breezyweather.sources.qweather.json.QWeatherWarningResult
import retrofit2.Retrofit
import javax.inject.Inject

class QWeatherService @Inject constructor(
    @ApplicationContext context: Context,
    client: Retrofit.Builder
) : HttpSource(), MainWeatherSource, SecondaryWeatherSource, LocationSearchSource, ReverseGeocodingSource, ConfigurableSource {

    override val id = "qweather"
    override val name = "QWeather"
    override val privacyPolicyUrl = "https://www.qweather.com/en/terms/privacy"
    override val color = Color.rgb(24, 106, 194)

    override val weatherAttribution = "QWeather"
    override val airQualityAttribution = "QWeather"
    override val pollenAttribution = null
    override val minutelyAttribution = "QWeather"
    override val alertAttribution = "QWeather"
    override val normalsAttribution = null
    override val locationSearchAttribution = "QWeather"

    override val supportedFeaturesInMain = listOf(
        SecondaryWeatherSourceFeature.FEATURE_AIR_QUALITY,
        SecondaryWeatherSourceFeature.FEATURE_MINUTELY,
        SecondaryWeatherSourceFeature.FEATURE_ALERT
    )

    override val supportedFeaturesInSecondary = listOf(
        SecondaryWeatherSourceFeature.FEATURE_AIR_QUALITY,
        SecondaryWeatherSourceFeature.FEATURE_ALERT,
        SecondaryWeatherSourceFeature.FEATURE_MINUTELY
    )

    private val mApi by lazy {
        client
            .baseUrl(QWEATHER_BASE_URL)
            .build()
            .create(QWeatherApi::class.java)
    }
    private val mGeoApi by lazy {
        client
            .baseUrl(QWEATHER_GEO_URL)
            .build()
            .create(QWeatherGeoApi::class.java)
    }

    // CONFIG
    private val config = SourceConfigStore(context, id)
    private var apikey: String
        set(value) {
            config.edit().putString("apikey", value).apply()
        }
        get() = config.getString("apikey", null) ?: ""

    private fun getApiKeyOrDefault(): String {
        return apikey.ifEmpty { BuildConfig.QWEATHER_KEY }
    }

    override val isConfigured
        get() = getApiKeyOrDefault().isNotEmpty()

    override val isRestricted
        get() = apikey.isEmpty()

    override fun getPreferences(context: Context): List<Preference> {
        return listOf(
            EditTextPreference(
                titleId = R.string.settings_weather_source_qweather_api_key,
                summary = { c, content ->
                    content.ifEmpty {
                        c.getString(R.string.settings_source_default_value)
                    }
                },
                content = apikey,
                onValueChanged = {
                    apikey = it
                }
            )
        )
    }

    private fun getLanguage(context: Context): String {
        return if (supportedLanguages.contains(context.currentLocale.codeWithCountry)) {
            context.currentLocale.codeWithCountry
        } else if (supportedLanguages.contains(context.currentLocale.code)) {
            context.currentLocale.code
        } else if (context.currentLocale.code == "zh-tw") {
            "zh-hant"
        } else "en"
    }

    override fun requestWeather(context: Context, location: Location, ignoreFeatures: List<SecondaryWeatherSourceFeature>): Observable<WeatherWrapper> {
        if (!isConfigured) {
            return Observable.error(ApiKeyMissingException())
        }
        if (location.cityId.isNullOrEmpty()) {
            return Observable.error(InvalidLocationException())
        }
        val apiKey = getApiKeyOrDefault()
        val lang = getLanguage(context)

        val daily = mApi.getWeather7d(location.cityId!!, apiKey, lang)
        val hourly = mApi.getWeather24h(location.cityId!!, apiKey, lang)
        val airDaily = mApi.getAir5d(location.cityId!!, apiKey, lang)
        val now = mApi.getWeatherNow(location.cityId!!, apiKey, lang)

        val air = if (!ignoreFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_AIR_QUALITY)) {
            mApi.getAirNow(location.cityId!!, apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherAirResult())
            }
        }
        val warning = if (!ignoreFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_ALERT)) {
            mApi.getWarningNow(location.cityId!!, apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherWarningResult())
            }
        }
        val minutely = if (!ignoreFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_MINUTELY)) {
            mApi.getMinutely("${location.longitude},${location.latitude}", apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherMinuteResult())
            }
        }

        return Observable.zip(daily, hourly, airDaily, now, air, warning, minutely) { dailyResult: QWeatherDailyResult,
                                                                                      hourlyResult: QWeatherHourlyResult,
                                                                                      airDailyResult: QWeatherAirDailyResult,
                                                                                      nowResult: QWeatherNowResult,
                                                                                      airResult: QWeatherAirResult,
                                                                                      warningResult: QWeatherWarningResult,
                                                                                      minutelyResult: QWeatherMinuteResult
            ->
            convert(
                location,
                dailyResult,
                hourlyResult,
                airDailyResult,
                nowResult,
                airResult,
                warningResult,
                minutelyResult,
                lang
            )
        }
    }

    override fun requestSecondaryWeather(context: Context, location: Location, requestedFeatures: List<SecondaryWeatherSourceFeature>): Observable<SecondaryWeatherWrapper> {
        if (!isConfigured) {
            return Observable.error(ApiKeyMissingException())
        }
        if (location.cityId.isNullOrEmpty()) {
            return Observable.error(InvalidLocationException())
        }
        val apiKey = getApiKeyOrDefault()
        val lang = getLanguage(context)

        val air = if (requestedFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_AIR_QUALITY)) {
            mApi.getAirNow(location.cityId!!, apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherAirResult())
            }
        }
        val warning = if (requestedFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_ALERT)) {
            mApi.getWarningNow(location.cityId!!, apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherWarningResult())
            }
        }
        val minutely = if (requestedFeatures.contains(SecondaryWeatherSourceFeature.FEATURE_MINUTELY)) {
            mApi.getMinutely("${location.longitude},${location.latitude}", apiKey, lang)
        } else {
            Observable.create { emitter ->
                emitter.onNext(QWeatherMinuteResult())
            }
        }

        return Observable.zip(air, warning, minutely) {
                airResult: QWeatherAirResult,
                warningResult: QWeatherWarningResult,
                minutelyResult: QWeatherMinuteResult,
            ->
            convertSecondary(
                airResult,
                warningResult,
                minutelyResult
            )
        }
    }

    override fun requestLocationSearch(context: Context, query: String): Observable<List<Location>> {
        return mGeoApi.cityLookup(query, getApiKeyOrDefault(), getLanguage(context))
            .map { result ->
                val locationList = mutableListOf<Location>()
                result.location?.sortedBy(QWeatherLocation::rank)?.forEach {
                    locationList.add(convertLocation(null, it))
                }
                locationList
            }
    }

    override fun requestReverseGeocodingLocation(context: Context, location: Location): Observable<List<Location>> {
        return mGeoApi.cityLookup("${location.longitude},${location.latitude}", getApiKeyOrDefault(), getLanguage(context))
            .map { result ->
                val locationList = mutableListOf<Location>()
                result.location?.forEach {
                    locationList.add(convertLocation(location, it))
                }
                locationList
            }
    }

    companion object {
        const val QWEATHER_BASE_URL = "https://devapi.qweather.com/v7/"
        const val QWEATHER_GEO_URL = "https://geoapi.qweather.com/v2/"
        private val supportedLanguages = setOf(
            "zh-hans", "zh",
            "zh-hant",
            "en",
            "de",
            "es",
            "fr",
            "it",
            "ja",
            "ko",
            "ru",
            "hi",
            "th",
            "ar",
            "pt",
            "bn",
            "ms",
            "nl",
            "el",
            "la",
            "sv",
            "id",
            "pl",
            "tr",
            "cs",
            "et",
            "vi",
            "fil",
            "fi",
            "he",
            "is",
            "nb"
        )
    }
}