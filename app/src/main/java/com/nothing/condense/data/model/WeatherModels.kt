package com.nothing.condense.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val current: CurrentWeather? = null,
    val hourly: HourlyWeather? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentWeather(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("uv_index") val uvIndex: Double? = null
)

@Serializable
data class HourlyWeather(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperatures: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbabilities: List<Int> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    @SerialName("uv_index") val uvIndices: List<Double> = emptyList(),
    @SerialName("weather_code") val weatherCodes: List<Int> = emptyList()
)

@Serializable
data class DailyWeather(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val maxTemperatures: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val minTemperatures: List<Double> = emptyList(),
    @SerialName("uv_index_max") val maxUvIndices: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val maxPrecipitationProbabilities: List<Int> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSums: List<Double> = emptyList(),
    @SerialName("weather_code") val weatherCodes: List<Int> = emptyList()
)

@Serializable
data class RainViewerResponse(
    val version: String? = null,
    val generated: Long? = null,
    val host: String? = null,
    val radar: RadarTimeline? = null
)

@Serializable
data class RadarTimeline(
    val past: List<RadarFrame> = emptyList(),
    val nowcast: List<RadarFrame> = emptyList()
)

@Serializable
data class RadarFrame(
    val time: Long,
    val path: String
)

/**
 * Calculated domain summary ready for UI, Widget, and Lockscreen consumption.
 */
data class WeatherSummary(
    val locationName: String,
    val currentTemp: Int,
    val todayHigh: Int,
    val todayLow: Int,
    val feelsLike: Int,
    val currentUv: Double,
    val maxUvToday: Double,
    val uvCategory: String,
    val nextRainHeadline: String,
    val nextRainSubtext: String,
    val isRainingNow: Boolean,
    val conditionDescription: String,
    val conditionEmoji: String,
    val hourlyForecast: List<HourlyItem>,
    val dailyForecast: List<DailyItem> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class HourlyItem(
    val timeLabel: String,
    val temp: Int,
    val precipProb: Int,
    val uvIndex: Double,
    val emoji: String
)

data class DailyItem(
    val dayLabel: String,
    val dateLabel: String,
    val maxTemp: Int,
    val minTemp: Int,
    val precipProb: Int,
    val maxUv: Double,
    val emoji: String,
    val conditionDesc: String
)

