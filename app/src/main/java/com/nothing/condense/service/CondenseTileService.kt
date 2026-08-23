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
            val rainTag = getRainTag(summary.isRainingNow, summary.nextRainHeadline)
            tile.label = "${summary.conditionEmoji} ${summary.currentTemp}° - ${summary.todayHigh}/${summary.todayLow}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "${summary.currentUv.toInt()} UV ${summary.maxUvToday.toInt()} - 🌧️ $rainTag"
                tile.contentDescription = "${summary.locationName}: ${summary.currentTemp} degrees, ${summary.todayHigh} high, ${summary.todayLow} low, UV ${summary.currentUv.toInt()} max ${summary.maxUvToday.toInt()}, rain $rainTag"
            }
            tile.updateTile()
        } else {
            tile.label = "⛅ --° - --/--"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "0 UV 0 - 🌧️ -"
            }
            tile.updateTile()

            CoroutineScope(Dispatchers.IO).launch {
                WeatherRepository.getInstance(applicationContext).refreshWeather()
                val refreshed = WeatherRepository.getInstance(applicationContext).currentSummary.value
                if (refreshed != null) {
                    val freshTile = qsTile ?: return@launch
                    val rainTag = getRainTag(refreshed.isRainingNow, refreshed.nextRainHeadline)
                    freshTile.label = "${refreshed.conditionEmoji} ${refreshed.currentTemp}° - ${refreshed.todayHigh}/${refreshed.todayLow}"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        freshTile.subtitle = "${refreshed.currentUv.toInt()} UV ${refreshed.maxUvToday.toInt()} - 🌧️ $rainTag"
                    }
                    freshTile.updateTile()
                }
            }
        }
    }

    private fun getRainTag(isRaining: Boolean, headline: String): String {
        if (isRaining) return "Now"
        val lower = headline.lowercase()
        val minMatch = Regex("""in (\d+)\s*m""").find(lower)
        if (minMatch != null) return "${minMatch.groupValues[1]}m"

        val hrMatch = Regex("""in (\d+)\s*h""").find(lower)
        if (hrMatch != null) return "${hrMatch.groupValues[1]}h"

        val dayMatch = Regex("""in (\d+)\s*day""").find(lower)
        if (dayMatch != null) return "${dayMatch.groupValues[1]}d"

        if (lower.contains("tomorrow")) return "Tmrw"
        if (lower.contains("no rain") || lower.contains("clear") || lower.isEmpty()) return "-"

        return "-"
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
