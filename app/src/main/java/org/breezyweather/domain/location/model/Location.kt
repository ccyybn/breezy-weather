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
        val riseTime = astro?.riseDate?.time
        val setTime = astro?.setDate?.time
        if (riseTime == null || setTime == null) {
            return false
        }
        val currentTime = System.currentTimeMillis()
        return currentTime >= riseTime && currentTime < setTime
    }
