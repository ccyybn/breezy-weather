package org.breezyweather.weather.json.accu

import kotlinx.serialization.Serializable

@Serializable
data class AccuAirQualityConcentration(
    val value: Double?
)
