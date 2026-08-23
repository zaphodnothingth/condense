package com.nothing.condense.data.api

import com.nothing.condense.data.model.OpenMeteoResponse
import com.nothing.condense.data.model.RainViewerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WeatherApiService(
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) {

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        useFahrenheit: Boolean = true
    ): OpenMeteoResponse {
        val tempUnit = if (useFahrenheit) "fahrenheit" else "celsius"
        val precipUnit = if (useFahrenheit) "inch" else "mm"
        val url = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,uv_index" +
                "&hourly=temperature_2m,precipitation_probability,precipitation,uv_index,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max,precipitation_sum,weather_code" +
                "&forecast_days=16" +
                "&temperature_unit=$tempUnit&precipitation_unit=$precipUnit&timezone=auto"

        return client.get(url).body()
    }

    suspend fun fetchRadarFrames(): RainViewerResponse {
        val url = "https://api.rainviewer.com/public/weather-maps.json"
        return client.get(url).body()
    }
}
