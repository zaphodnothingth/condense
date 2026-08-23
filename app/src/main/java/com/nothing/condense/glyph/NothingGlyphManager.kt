package com.nothing.condense.glyph

import android.content.Context
import android.os.Build
import android.util.Log
import com.nothing.condense.data.model.WeatherSummary

/**
 * Manages ambient Glyph lighting and progress countdowns for Nothing Phone hardware.
 */
object NothingGlyphManager {

    private const val TAG = "NothingGlyphManager"
    val isNothingDevice: Boolean by lazy {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        man.contains("nothing") || brand.contains("nothing") || model.startsWith("a0")
    }

    fun onWeatherUpdated(context: Context, summary: WeatherSummary?) {
        if (summary == null || !isNothingDevice) return

        try {
            val headline = summary.nextRainHeadline.lowercase()
            val isThunderstorm = summary.conditionEmoji.contains("⛈️") || summary.conditionDescription.lowercase().contains("thunder")

            // Check if rain is within 1 hour (e.g. "rain in 37m", "rain in < 1 hour", "raining now", "rain in 1h")
            val minutesLeft = parseMinutesLeft(headline, summary.isRainingNow)

            if (minutesLeft != null && minutesLeft in 0..60) {
                // Active 1-Hour Rain Alert
                val progressPercent = ((60 - minutesLeft) / 60.0 * 100.0).toInt().coerceIn(0, 100)
                Log.d(TAG, "Triggering Glyph Rain Alert: ${minutesLeft}m left ($progressPercent%), Thunderstorm: $isThunderstorm")
                triggerGlyphAlert(context, minutesLeft, progressPercent, isThunderstorm)
            } else {
                Log.d(TAG, "No immediate rain within 1 hour. Glyph idle.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Glyph integration fallback: ${e.message}")
        }
    }

    private fun triggerGlyphAlert(
        context: Context,
        minutesLeft: Int,
        progressPercent: Int,
        isThunderstorm: Boolean
    ) {
        // Safe Glyph notification / progress hook
        // Can be expanded with Nothing Glyph Developer Kit IPC or debug service
        Log.i(TAG, "Nothing Glyph Progress Set: $progressPercent% (Rain in ${minutesLeft}m, Storm: $isThunderstorm)")
    }

    private fun parseMinutesLeft(headline: String, isRainingNow: Boolean): Int? {
        if (isRainingNow) return 0
        val minMatch = Regex("""in (\d+)\s*m""").find(headline)
        if (minMatch != null) {
            return minMatch.groupValues[1].toIntOrNull()
        }
        if (headline.contains("< 1 hour")) return 30
        if (headline.contains("in 1h") || headline.contains("in 1 hour")) return 60
        return null
    }
}
