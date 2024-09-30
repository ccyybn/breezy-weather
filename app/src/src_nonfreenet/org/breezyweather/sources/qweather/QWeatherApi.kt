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

import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.qweather.json.QWeatherAirResult
import org.breezyweather.sources.qweather.json.QWeatherAirDailyResult
import org.breezyweather.sources.qweather.json.QWeatherDailyResult
import org.breezyweather.sources.qweather.json.QWeatherHourlyResult
import org.breezyweather.sources.qweather.json.QWeatherMinuteResult
import org.breezyweather.sources.qweather.json.QWeatherNowResult
import org.breezyweather.sources.qweather.json.QWeatherWarningResult
import retrofit2.http.GET
import retrofit2.http.Query

interface QWeatherApi {
    @GET("weather/now")
    fun getWeatherNow(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherNowResult>

    @GET("weather/7d")
    fun getWeather7d(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherDailyResult>

    @GET("weather/24h")
    fun getWeather24h(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherHourlyResult>

    @GET("air/now")
    fun getAirNow(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherAirResult>

    @GET("air/5d")
    fun getAir5d(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherAirDailyResult>

    @GET("warning/now")
    fun getWarningNow(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherWarningResult>

    @GET("minutely/5m")
    fun getMinutely(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("lang") lang: String
    ): Observable<QWeatherMinuteResult>
}
