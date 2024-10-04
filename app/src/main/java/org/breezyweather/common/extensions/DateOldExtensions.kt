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

package org.breezyweather.common.extensions

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RequiresApi(Build.VERSION_CODES.O)
data class MoonDates(val moonTimes: MoonTimes) {
    val rise: Date? = if (moonTimes.rise != null) Date.from(moonTimes.rise?.toInstant()) else null
    val set: Date? = if (moonTimes.set != null) Date.from(moonTimes.set?.toInstant()) else null
    val isAlwaysUp = moonTimes.isAlwaysUp
    val isAlwaysDown = moonTimes.isAlwaysDown
}


@RequiresApi(Build.VERSION_CODES.O)
data class SunDates(val sunTimes: SunTimes) {
    val rise: Date? = if (sunTimes.rise != null) Date.from(sunTimes.rise?.toInstant()) else null
    val set: Date? = if (sunTimes.set != null) Date.from(sunTimes.set?.toInstant()) else null
    val isAlwaysUp = sunTimes.isAlwaysUp
    val isAlwaysDown = sunTimes.isAlwaysDown
}

@SuppressLint("NewApi")
fun Date.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
    return LocalDateTime.ofInstant(this.toInstant(), zoneId)
}

@SuppressLint("NewApi")
fun LocalDateTime.toDate(zoneId: ZoneId): Date {
    return Date.from(this.atZone(zoneId).toInstant())
}

@SuppressLint("NewApi")
fun LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

@RequiresApi(Build.VERSION_CODES.O)
fun MoonTimes.toDate(): MoonDates {
    return MoonDates(this)
}

@RequiresApi(Build.VERSION_CODES.O)
fun SunTimes.toDate(): SunDates {
    return SunDates(this)
}

/**
 * The functions below make use of old java.util.* that should be replaced with android.icu
 * counterparts, introduced in Android SDK 24
 */

fun Date.toCalendarWithTimeZone(zone: TimeZone): Calendar {
    return Calendar.getInstance().also {
        it.time = this
        it.timeZone = zone
    }
}


/**
 * Get a date at midnight on a specific timezone from a formatted date
 * @this formattedDate in yyyy-MM-dd format
 * @param timeZoneP
 * @return Date
 */
fun String.toDateNoHour(timeZoneP: TimeZone = TimeZone.getDefault()): Date? {
    if (this.isEmpty() || this.length < 10) return null
    return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = timeZoneP }
        .parse(this)
}

fun Date.toTimezoneNoHour(timeZone: TimeZone = TimeZone.getDefault()): Date? {
    return this.toCalendarWithTimeZone(timeZone).apply {
        set(Calendar.YEAR, this.get(Calendar.YEAR))
        set(Calendar.MONTH, this.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, this.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}

@Deprecated("Use ICU functions instead")
fun Date.getFormattedDate(
    pattern: String,
    timeZone: TimeZone?,
    locale: Locale
): String {
    return SimpleDateFormat(
        pattern, locale
    ).apply {
        setTimeZone(timeZone ?: TimeZone.getDefault())
    }.format(this)
}
