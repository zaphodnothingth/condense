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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

import android.location.LocationManager
import com.google.android.gms.location.Priority
import kotlinx.coroutines.withTimeoutOrNull

class WeatherRepository private constructor(private val context: Context) {

    private val apiService = WeatherApiService()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationPrefs = context.getSharedPreferences("condense_location_prefs", Context.MODE_PRIVATE)

    private val _currentSummary = MutableStateFlow<WeatherSummary?>(null)
    val currentSummary: StateFlow<WeatherSummary?> = _currentSummary.asStateFlow()

    private val _radarFrames = MutableStateFlow<List<RadarFrame>>(emptyList())
    val radarFrames: StateFlow<List<RadarFrame>> = _radarFrames.asStateFlow()

    private val _radarHost = MutableStateFlow<String>("https://tilecache.rainviewer.com")
    val radarHost: StateFlow<String> = _radarHost.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>>(
        Pair(
            locationPrefs.getFloat("last_lat", 36.7682f).toDouble(),
            locationPrefs.getFloat("last_lon", -76.2875f).toDouble()
        )
    )
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun refreshWeather(): WeatherSummary? = withContext(Dispatchers.IO) {
        try {
            var lat = locationPrefs.getFloat("last_lat", 36.7682f).toDouble()
            var lon = locationPrefs.getFloat("last_lon", -76.2875f).toDouble()
            var locationName = locationPrefs.getString("last_name", "Chesapeake") ?: "Chesapeake"

            try {
                // 1. Try active GPS fix with timeout
                val loc: Location? = withTimeoutOrNull(5000) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).awaitTask()
                } ?: fusedLocationClient.lastLocation.awaitTask() ?: run {
                    val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    _currentLocation.value = Pair(lat, lon)

                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        locationName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: locationName
                    }

                    // Persist resolved location
                    locationPrefs.edit()
                        .putFloat("last_lat", lat.toFloat())
                        .putFloat("last_lon", lon.toFloat())
                        .putString("last_name", locationName)
                        .apply()
                }
            } catch (e: Exception) {
                // Location permission or timeout fallback to saved location
            }

            val (response, aqiResponse) = coroutineScope {
                val weatherDef = async { apiService.fetchWeather(lat, lon, useFahrenheit = true) }
                val aqiDef = async {
                    try { apiService.fetchAirQuality(lat, lon) } catch (e: Exception) { null }
                }
                Pair(weatherDef.await(), aqiDef.await())
            }

            val summary = RainEngine.processForecast(response, locationName, aqiResponse)

            _currentSummary.value = summary

            // Update lock screen notification & Quick Settings Tile
            LockScreenNotificationManager.updateNotification(context, summary)
            com.nothing.condense.service.CondenseTileService.requestTileUpdate(context)

            // Trigger Nothing Glyph ambient rain / lightning countdown
            com.nothing.condense.glyph.NothingGlyphManager.onWeatherUpdated(context, summary)

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

