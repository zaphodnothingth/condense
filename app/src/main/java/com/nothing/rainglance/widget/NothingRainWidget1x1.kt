package com.nothing.rainglance.widget

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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nothing.rainglance.data.WeatherRepository
import com.nothing.rainglance.ui.MainActivity

class NothingRainWidget1x1 : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WeatherRepository.getInstance(context)
        var summary = repo.currentSummary.value
        if (summary == null) {
            summary = repo.refreshWeather()
        }

        provideContent {
            GlanceTheme {
                val temp = summary?.currentTemp ?: 72
                val isRaining = summary?.isRainingNow == true
                val headline = summary?.nextRainHeadline ?: "No rain"

                // Extract short rain tag like "3h", "Now", "Dry"
                val shortRain = when {
                    isRaining -> "Rain Now"
                    headline.contains("In ", ignoreCase = true) -> headline.replace("Rain in ", "").replace("In ", "")
                    headline.contains("tomorrow", ignoreCase = true) -> "Tmrw"
                    else -> "Dry"
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF141416)))
                        .cornerRadius(20.dp)
                        .padding(8.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(5.dp)
                                    .background(ColorProvider(if (isRaining) Color(0xFFFF3B30) else Color(0xFFD71920)))
                                    .cornerRadius(2.5.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "$temp°",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = if (isRaining) "🌧️ Now" else "☔ $shortRain",
                            style = TextStyle(
                                color = ColorProvider(if (isRaining) Color(0xFFFF453A) else Color(0xFF0A84FF)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
