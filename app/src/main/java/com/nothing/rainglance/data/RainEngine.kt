package com.nothing.rainglance.data

import com.nothing.rainglance.data.model.HourlyItem
import com.nothing.rainglance.data.model.OpenMeteoResponse
import com.nothing.rainglance.data.model.WeatherSummary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object RainEngine {

    fun processForecast(
        response: OpenMeteoResponse,
        locationName: String = "Current Location"
    ): WeatherSummary {
        val current = response.current
        val hourly = response.hourly
        val daily = response.daily

        val currentTemp = current?.temperature?.roundToInt() ?: 0
        val feelsLike = current?.apparentTemperature?.roundToInt() ?: currentTemp
        val todayHigh = daily?.maxTemperatures?.firstOrNull()?.roundToInt() ?: currentTemp
        val todayLow = daily?.minTemperatures?.firstOrNull()?.roundToInt() ?: currentTemp
        val maxUv = daily?.maxUvIndices?.firstOrNull() ?: (current?.uvIndex ?: 0.0)
        val currentUv = current?.uvIndex ?: 0.0

        val (isRaining, rainHeadline, rainSubtext) = calculateNextRain(current?.precipitation, hourly)
        val (emoji, desc) = getWeatherCodeDetails(current?.weatherCode ?: 0)

        val hourlyItems = buildHourlyList(hourly)

        return WeatherSummary(
            locationName = locationName,
            currentTemp = currentTemp,
            todayHigh = todayHigh,
            todayLow = todayLow,
            feelsLike = feelsLike,
            currentUv = currentUv,
            maxUvToday = maxUv,
            uvCategory = getUvCategory(currentUv),
            nextRainHeadline = rainHeadline,
            nextRainSubtext = rainSubtext,
            isRainingNow = isRaining,
            conditionDescription = desc,
            conditionEmoji = emoji,
            hourlyForecast = hourlyItems
        )
    }

    fun calculateNextRain(
        currentPrecipitation: Double?,
        hourly: com.nothing.rainglance.data.model.HourlyWeather?,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): Triple<Boolean, String, String> {
        val isRainingNow = (currentPrecipitation ?: 0.0) > 0.02

        if (isRainingNow) {
            return Triple(true, "Raining Now", "Ongoing precipitation")
        }

        if (hourly == null || hourly.time.isEmpty()) {
            return Triple(false, "No rain data", "Check connection")
        }

        val now = currentTime
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        for (i in hourly.time.indices) {
            val timeStr = hourly.time[i]
            val forecastTime = try {
                LocalDateTime.parse(timeStr, formatter)
            } catch (e: Exception) {
                continue
            }

            if (forecastTime.isBefore(now.minusMinutes(30))) {
                continue // Skip past hours
            }

            val prob = hourly.precipitationProbabilities.getOrNull(i) ?: 0
            val amount = hourly.precipitation.getOrNull(i) ?: 0.0

            // Trigger criteria: >= 30% chance or measurable precipitation
            if (prob >= 30 || amount > 0.02) {
                val minutesDiff = ChronoUnit.MINUTES.between(now, forecastTime)
                val hoursDiff = ((minutesDiff + 30) / 60).coerceAtLeast(0)
                val headline: String
                val subtext: String

                if (minutesDiff <= 45) {
                    headline = "Rain in < 1 hour"
                    subtext = "${prob}% chance ($amount in)"
                } else if (hoursDiff < 24) {
                    headline = "Rain in ${hoursDiff}h"
                    val hourOfDay = forecastTime.format(DateTimeFormatter.ofPattern("h a"))
                    subtext = "Today at $hourOfDay (${prob}%)"
                } else if (hoursDiff < 48) {
                    val hourOfDay = forecastTime.format(DateTimeFormatter.ofPattern("h a"))
                    headline = "Rain tomorrow"
                    subtext = "Around $hourOfDay (${prob}%)"
                } else {
                    val dayName = forecastTime.format(DateTimeFormatter.ofPattern("EEE h a"))
                    val daysDiff = ChronoUnit.DAYS.between(now.toLocalDate(), forecastTime.toLocalDate())
                    headline = "Rain in ${daysDiff} days"
                    subtext = "$dayName (${prob}%)"
                }

                return Triple(false, headline, subtext)
            }
        }

        return Triple(false, "No rain expected", "Clear for next 7 days")
    }

    fun getUvCategory(uv: Double): String {
        return when {
            uv < 3.0 -> "Low"
            uv < 6.0 -> "Moderate"
            uv < 8.0 -> "High"
            uv < 11.0 -> "Very High"
            else -> "Extreme"
        }
    }

    fun getWeatherCodeDetails(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("☀️", "Clear Sky")
            1 -> Pair("🌤️", "Mainly Clear")
            2 -> Pair("⛅", "Partly Cloudy")
            3 -> Pair("☁️", "Overcast")
            45, 48 -> Pair("🌫️", "Foggy")
            51, 53, 55 -> Pair("🌦️", "Drizzle")
            56, 57 -> Pair("🌧️", "Freezing Drizzle")
            61, 63 -> Pair("🌧️", "Rain")
            65 -> Pair("🌧️", "Heavy Rain")
            66, 67 -> Pair("🌨️", "Freezing Rain")
            71, 73, 75 -> Pair("🌨️", "Snow")
            77 -> Pair("❄️", "Snow Grains")
            80, 81, 82 -> Pair("🌦️", "Rain Showers")
            85, 86 -> Pair("🌨️", "Snow Showers")
            95 -> Pair("⛈️", "Thunderstorm")
            96, 99 -> Pair("⛈️", "Thunderstorm w/ Hail")
            else -> Pair("🌡️", "Fair")
        }
    }

    private fun buildHourlyList(hourly: com.nothing.rainglance.data.model.HourlyWeather?): List<HourlyItem> {
        if (hourly == null) return emptyList()
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val items = mutableListOf<HourlyItem>()

        for (i in hourly.time.indices) {
            val timeStr = hourly.time[i]
            val forecastTime = try {
                LocalDateTime.parse(timeStr, formatter)
            } catch (e: Exception) {
                continue
            }

            if (forecastTime.isBefore(now.minusMinutes(45))) continue

            val hourLabel = forecastTime.format(DateTimeFormatter.ofPattern("h a"))
            val temp = hourly.temperatures.getOrNull(i)?.roundToInt() ?: 0
            val prob = hourly.precipitationProbabilities.getOrNull(i) ?: 0
            val uv = hourly.uvIndices.getOrNull(i) ?: 0.0
            val code = hourly.weatherCodes.getOrNull(i) ?: 0
            val emoji = getWeatherCodeDetails(code).first

            items.add(HourlyItem(hourLabel, temp, prob, uv, emoji))
            if (items.size >= 24) break
        }
        return items
    }
}
