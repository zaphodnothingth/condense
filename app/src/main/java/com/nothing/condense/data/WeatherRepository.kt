package com.nothing.condense.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.nothing.condense.data.api.WeatherApiService
import com.nothing.condense.data.model.RadarFrame
import com.nothing.condense.data.model.WeatherSummary
import com.nothing.condense.service.LockScreenNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class WeatherRepository private constructor(private val context: Context) {

    private val apiService = WeatherApiService()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _currentSummary = MutableStateFlow<WeatherSummary?>(null)
    val currentSummary: StateFlow<WeatherSummary?> = _currentSummary.asStateFlow()

    private val _radarFrames = MutableStateFlow<List<RadarFrame>>(emptyList())
    val radarFrames: StateFlow<List<RadarFrame>> = _radarFrames.asStateFlow()

    private val _radarHost = MutableStateFlow<String>("https://tilecache.rainviewer.com")
    val radarHost: StateFlow<String> = _radarHost.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>>(Pair(37.7749, -122.4194))
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun refreshWeather(): WeatherSummary? = withContext(Dispatchers.IO) {
        try {
            var lat = 37.7749
            var lon = -122.4194
            var locationName = "San Francisco"

            try {
                val loc: Location? = fusedLocationClient.lastLocation.awaitTask()
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    _currentLocation.value = Pair(lat, lon)

                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        locationName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Current Location"
                    }
                }
            } catch (e: Exception) {
                // Location permission or timeout fallback
            }

            val response = apiService.fetchWeather(lat, lon, useFahrenheit = true)
            val summary = RainEngine.processForecast(response, locationName)

            _currentSummary.value = summary

            // Update lock screen notification
            LockScreenNotificationManager.updateNotification(context, summary)

            // Also fetch radar frames
            try {
                val radarResp = apiService.fetchRadarFrames()
                if (radarResp.host != null) {
                    _radarHost.value = radarResp.host
                }
                val allFrames = (radarResp.radar?.past ?: emptyList()) + (radarResp.radar?.nowcast ?: emptyList())
                _radarFrames.value = allFrames
            } catch (e: Exception) {
                // Radar optional fallback
            }

            summary
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener {
        if (cont.isActive) cont.resume(null)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

