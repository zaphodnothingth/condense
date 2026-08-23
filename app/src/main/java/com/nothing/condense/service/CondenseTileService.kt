package com.nothing.condense.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.nothing.condense.R
import com.nothing.condense.data.WeatherRepository
import com.nothing.condense.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class CondenseTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    appIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(appIntent)
        }

        // Trigger background refresh in parallel
        CoroutineScope(Dispatchers.IO).launch {
            WeatherRepository.getInstance(applicationContext).refreshWeather()
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val summary = WeatherRepository.getInstance(this).currentSummary.value

        tile.state = Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_weather)

        if (summary != null) {
            tile.label = "${summary.currentTemp}° ${summary.conditionEmoji}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rainSub = if (summary.isRainingNow) "Raining now" else summary.nextRainHeadline
                tile.subtitle = "${summary.todayHigh}/${summary.todayLow} · UV ${summary.currentUv.toInt()} · $rainSub"
                tile.contentDescription = "${summary.locationName}: ${summary.currentTemp} degrees, ${summary.conditionDescription}"
            }
            tile.updateTile()
        } else {
            tile.label = "Condense"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Updating weather..."
            }
            tile.updateTile()

            CoroutineScope(Dispatchers.IO).launch {
                WeatherRepository.getInstance(applicationContext).refreshWeather()
                val refreshed = WeatherRepository.getInstance(applicationContext).currentSummary.value
                if (refreshed != null) {
                    val freshTile = qsTile ?: return@launch
                    freshTile.label = "${refreshed.currentTemp}° ${refreshed.conditionEmoji}"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val rainSub = if (refreshed.isRainingNow) "Raining now" else refreshed.nextRainHeadline
                        freshTile.subtitle = "${refreshed.todayHigh}/${refreshed.todayLow} · UV ${refreshed.currentUv.toInt()} · $rainSub"
                    }
                    freshTile.updateTile()
                }
            }
        }
    }

    companion object {
        fun requestTileUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    requestListeningState(context, ComponentName(context, CondenseTileService::class.java))
                } catch (e: Exception) {
                    // Ignore on non-supported platforms
                }
            }
        }
    }
}
