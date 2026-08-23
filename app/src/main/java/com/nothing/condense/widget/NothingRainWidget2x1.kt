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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nothing.condense.data.WeatherRepository
import com.nothing.condense.ui.MainActivity

class NothingRainWidget2x1 : GlanceAppWidget() {

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
                val rainSubtext = summary?.nextRainSubtext ?: "Clear today"
                val isRaining = summary?.isRainingNow == true

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF141416)))
                        .cornerRadius(22.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header Row: Temp + High/Low + UV
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Red Dot Indicator for Nothing OS accent
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .background(ColorProvider(if (isRaining) Color(0xFFFF3B30) else Color(0xFFD71920)))
                                    .cornerRadius(3.dp)
                            ) {}

                            Spacer(modifier = GlanceModifier.width(6.dp))

                            Text(
                                text = "$temp°",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Text(
                                text = "H:$high° L:$low°",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8E8E93)),
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Text(
                                text = "• UV ${uv.toInt()} (Max ${maxUv.toInt()})",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE5E5EA)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Rain / Condition Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRaining) "🌧️ $rainHeadline" else "☔ $rainHeadline",
                                style = TextStyle(
                                    color = ColorProvider(if (isRaining) Color(0xFFFF453A) else Color(0xFF0A84FF)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.width(6.dp))

                            Text(
                                text = "· $rainSubtext",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFA1A1A6)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
