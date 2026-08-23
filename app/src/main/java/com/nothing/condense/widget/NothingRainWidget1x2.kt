package com.nothing.condense.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nothing.condense.data.WeatherRepository
import com.nothing.condense.ui.MainActivity

class NothingRainWidget1x2 : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WeatherRepository.getInstance(context)
        var summary = repo.currentSummary.value
        if (summary == null) {
            summary = repo.refreshWeather()
        }

        provideContent {
            GlanceTheme {
                val temp = summary?.currentTemp ?: 72
                val high = summary?.todayHigh ?: 80
                val low = summary?.todayLow ?: 60
                val uv = summary?.currentUv ?: 4.0
                val maxUv = summary?.maxUvToday ?: 7.0
                val rainHeadline = summary?.nextRainHeadline ?: "No rain expected"
                val isRaining = summary?.isRainingNow == true
                val conditionEmoji = summary?.conditionEmoji ?: "⛅"
                val rainTag = getRainTag(isRaining, rainHeadline)

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF141416)))
                        .cornerRadius(20.dp)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Row 1: ⛅ 77°
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = conditionEmoji,
                                style = TextStyle(fontSize = 15.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(3.dp))
                            Text(
                                text = "$temp°",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(5.dp))

                        // Row 2: 84/69
                        Text(
                            text = "$high/$low",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF8E8E93)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(5.dp))

                        // Row 3: 0 UV 7
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uv.toInt()}",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(2.dp))
                            Text(
                                text = "UV",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8E8E93)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(2.dp))
                            Text(
                                text = "${maxUv.toInt()}",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(5.dp))

                        // Row 4: 🌧️ 5d
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌧️",
                                style = TextStyle(fontSize = 14.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(3.dp))
                            Text(
                                text = rainTag,
                                style = TextStyle(
                                    color = ColorProvider(
                                        when {
                                            isRaining -> Color(0xFFFF453A)
                                            rainTag != "-" -> Color(0xFF0A84FF)
                                            else -> Color(0xFF8E8E93)
                                        }
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
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
}
