package com.nothing.rainglance

import com.nothing.rainglance.data.RainEngine
import com.nothing.rainglance.data.model.HourlyWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RainEngineTest {

    @Test
    fun testRainingNow() {
        val (isRaining, headline, subtext) = RainEngine.calculateNextRain(
            currentPrecipitation = 0.25,
            hourly = null
        )
        assertTrue(isRaining)
        assertEquals("Raining Now", headline)
        assertEquals("Ongoing precipitation", subtext)
    }

    @Test
    fun testNoRainInForecast() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val times = (0..24).map { now.plusHours(it.toLong()).format(formatter) }
        val probs = List(25) { 0 }
        val amounts = List(25) { 0.0 }

        val hourly = HourlyWeather(
            time = times,
            precipitationProbabilities = probs,
            precipitation = amounts
        )

        val (isRaining, headline, _) = RainEngine.calculateNextRain(
            currentPrecipitation = 0.0,
            hourly = hourly
        )
        assertEquals(false, isRaining)
        assertEquals("No rain expected", headline)
    }

    @Test
    fun testRainInUpcomingHours() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val times = (0..12).map { now.plusHours(it.toLong()).format(formatter) }
        val probs = listOf(0, 5, 10, 80, 70, 0, 0, 0, 0, 0, 0, 0, 0)
        val amounts = listOf(0.0, 0.0, 0.0, 0.15, 0.10, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        val hourly = HourlyWeather(
            time = times,
            precipitationProbabilities = probs,
            precipitation = amounts
        )

        val (isRaining, headline, subtext) = RainEngine.calculateNextRain(
            currentPrecipitation = 0.0,
            hourly = hourly,
            currentTime = now
        )
        assertEquals(false, isRaining)
        assertEquals("Rain in 3h", headline)
        assertTrue(subtext.contains("80%"))
    }

    @Test
    fun testUvCategoryLevels() {
        assertEquals("Low", RainEngine.getUvCategory(1.5))
        assertEquals("Moderate", RainEngine.getUvCategory(4.2))
        assertEquals("High", RainEngine.getUvCategory(7.0))
        assertEquals("Very High", RainEngine.getUvCategory(9.5))
        assertEquals("Extreme", RainEngine.getUvCategory(12.0))
    }
}
