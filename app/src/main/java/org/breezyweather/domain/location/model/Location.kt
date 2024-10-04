package org.breezyweather.domain.location.model

import android.content.Context
import breezyweather.domain.location.model.Location
import org.breezyweather.R

fun Location.getPlace(context: Context, showCurrentPositionInPriority: Boolean = false): String {
    if (showCurrentPositionInPriority && isCurrentPosition) {
        return context.getString(R.string.location_current)
    }
    val builder = StringBuilder()
    builder.append(cityAndDistrict)
    if (builder.toString().isEmpty() && isCurrentPosition) {
        return context.getString(R.string.location_current)
    }
    return builder.toString()
}

val Location.isDaylight: Boolean
    get() {
        val astro = this.weather?.today?.sun
        var riseTime = astro?.riseDate?.time
        var setTime = astro?.setDate?.time
        var riseTimePre = astro?.riseDatePre?.time
        var setTimePre = astro?.setDatePre?.time
        if (riseTime == null || setTime == null) {
            riseTime = -1
            setTime = -1
        }
        if (riseTimePre == null || setTimePre == null) {
            riseTimePre = -1
            setTimePre = -1
        }
        val currentTime = System.currentTimeMillis()
        return (currentTime >= riseTime && currentTime < setTime) || (currentTime >= riseTimePre && currentTime < setTimePre)
    }
