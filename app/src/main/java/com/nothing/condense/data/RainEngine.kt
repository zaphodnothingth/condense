package com.nothing.condense.data

import com.nothing.condense.data.model.DailyItem
import com.nothing.condense.data.model.HourlyItem
import com.nothing.condense.data.model.OpenMeteoResponse
import com.nothing.condense.data.model.WeatherSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object RainEngine {

    fun processForecast(
        response: OpenMeteoResponse,
        locationName: String = "Current Location",
        airQuality: com.nothing.condense.data.model.AirQualityResponse? = null
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
        val dailyItems = buildDailyList(daily)

        // Calculate Meteo Telemetry
        val meteoTelemetry = buildMeteoTelemetry(current, daily, airQuality)

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
            hourlyForecast = hourlyItems,
            dailyForecast = dailyItems,
            meteoTelemetry = meteoTelemetry
        )
    }

    private fun buildMeteoTelemetry(
        current: com.nothing.condense.data.model.CurrentWeather?,
        daily: com.nothing.condense.data.model.DailyWeather?,
        airQuality: com.nothing.condense.data.model.AirQualityResponse?
    ): com.nothing.condense.data.model.MeteoTelemetry {
        val aqiVal = airQuality?.current?.usAqi ?: 35
        val aqiCat = when {
            aqiVal <= 50 -> "Good"
            aqiVal <= 100 -> "Moderate"
            aqiVal <= 150 -> "Unhealthy (Sens)"
            aqiVal <= 200 -> "Unhealthy"
            else -> "Hazardous"
        }
        val pm25 = airQuality?.current?.pm25 ?: 8.5
        val pm10 = airQuality?.current?.pm10 ?: 12.0

        val windSpeed = current?.windSpeed?.roundToInt() ?: 8
        val windGusts = current?.windGusts?.roundToInt() ?: (windSpeed + 4)
        val windDeg = current?.windDirection ?: 0
        val windCard = getCardinalDirection(windDeg)

        val pressure = current?.surfacePressure?.roundToInt() ?: 1013
        val pressureTrend = when {
            pressure >= 1016 -> "Steady High (Fair)"
            pressure >= 1011 -> "Normal ↗"
            pressure >= 1005 -> "Falling ↘ (Storm chance)"
            else -> "Low ⚠️ (Severe front)"
        }

        val dewPoint = current?.dewPoint?.roundToInt() ?: 62
        val humidity = current?.humidity ?: 55
        val cloudCover = current?.cloudCover ?: 20

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val sunriseTime = daily?.sunrise?.firstOrNull()?.let {
            try { LocalDateTime.parse(it, formatter) } catch (e: Exception) { null }
        }
        val sunsetTime = daily?.sunset?.firstOrNull()?.let {
            try { LocalDateTime.parse(it, formatter) } catch (e: Exception) { null }
        }

        val sunriseStr = sunriseTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "6:30 AM"
        val sunsetStr = sunsetTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "7:45 PM"

        val daylightLeftStr = if (sunsetTime != null && now.isBefore(sunsetTime)) {
            val mins = ChronoUnit.MINUTES.between(now, sunsetTime)
            val h = mins / 60
            val m = mins % 60
            "${h}h ${m}m daylight left"
        } else {
            "Nighttime"
        }

        val goldenHourStr = sunsetTime?.minusMinutes(45)?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "7:00 PM"

        return com.nothing.condense.data.model.MeteoTelemetry(
            aqi = aqiVal,
            aqiCategory = aqiCat,
            pm25 = pm25,
            pm10 = pm10,
            windSpeedMph = windSpeed,
            windDirectionCardinal = windCard,
            windGustsMph = windGusts,
            pressureHpa = pressure,
            pressureTrend = pressureTrend,
            dewPoint = dewPoint,
            humidity = humidity,
            cloudCoverPercent = cloudCover,
            sunriseStr = sunriseStr,
            sunsetStr = sunsetStr,
            daylightLeftStr = daylightLeftStr,
            goldenHourStr = goldenHourStr
        )
    }

    private fun getCardinalDirection(degrees: Int): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((degrees % 360) + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    fun calculateNextRain(
        currentPrecipitation: Double?,
        hourly: com.nothing.condense.data.model.HourlyWeather?,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): Triple<Boolean, String, String> {
        val isRainingNow = (currentPrecipitation ?: 0.0) > 0.01

        if (isRainingNow) {
            return Triple(true, "Raining Now", "Ongoing precipitation")
        }

        if (hourly == null || hourly.time.isEmpty()) {
            return Triple(false, "No rain data", "Check connection")
        }

        val now = currentTime
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val rainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 85, 86, 95, 96, 99)

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
            val code = hourly.weatherCodes.getOrNull(i) ?: 0

            val minutesDiff = ChronoUnit.MINUTES.between(now, forecastTime)
            val hoursDiff = ((minutesDiff + 30) / 60).coerceAtLeast(0)

            // Sensitive trigger: 20%+ chance in next 24h, or 25%+ further out, or any rain weather code
            val isTrigger = (hoursDiff <= 24 && (prob >= 20 || amount > 0.005 || code in rainCodes)) ||
                    (hoursDiff > 24 && (prob >= 25 || amount > 0.01 || code in rainCodes))

            if (isTrigger) {
                val headline: String
                val subtext: String

                if (minutesDiff in 1..59) {
                    headline = "Rain in ${minutesDiff}m"
                    subtext = "${prob}% chance · 1-hour window"
                } else if (hoursDiff <= 1) {
                    headline = "Rain in 1h"
                    val hourOfDay = forecastTime.format(DateTimeFormatter.ofPattern("h a"))
                    subtext = "Around $hourOfDay (${prob}%)"
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

    private fun buildHourlyList(hourly: com.nothing.condense.data.model.HourlyWeather?): List<HourlyItem> {
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

    private fun buildDailyList(daily: com.nothing.condense.data.model.DailyWeather?): List<DailyItem> {
        if (daily == null || daily.time.isEmpty()) return emptyList()
        val now = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val items = mutableListOf<DailyItem>()

        for (i in daily.time.indices) {
            val dateStr = daily.time[i]
            val date = try {
                LocalDate.parse(dateStr, formatter)
            } catch (e: Exception) {
                continue
            }

            val dayLabel = when {
                date == now -> "Today"
                date == now.plusDays(1) -> "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("EEE"))
            }
            val dateLabel = date.format(DateTimeFormatter.ofPattern("MMM d"))
            val maxTemp = daily.maxTemperatures.getOrNull(i)?.roundToInt() ?: 0
            val minTemp = daily.minTemperatures.getOrNull(i)?.roundToInt() ?: 0
            val prob = daily.maxPrecipitationProbabilities.getOrNull(i) ?: 0
            val precipSum = daily.precipitationSums.getOrNull(i) ?: 0.0
            val uv = daily.maxUvIndices.getOrNull(i) ?: 0.0
            val code = daily.weatherCodes.getOrNull(i) ?: 0
            val (emoji, desc) = getWeatherCodeDetails(code)

            items.add(DailyItem(dayLabel, dateLabel, maxTemp, minTemp, prob, precipSum, uv, emoji, desc))
        }
        return items
    }
}
