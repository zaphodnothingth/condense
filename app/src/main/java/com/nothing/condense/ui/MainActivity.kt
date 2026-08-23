package com.nothing.condense.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nothing.condense.data.RainEngine
import com.nothing.condense.data.WeatherRepository
import com.nothing.condense.data.model.DailyItem
import com.nothing.condense.data.model.HourlyItem
import com.nothing.condense.data.model.MeteoTelemetry
import com.nothing.condense.data.model.WeatherSummary
import com.nothing.condense.ui.radar.RadarMapView
import com.nothing.condense.ui.theme.NothingRainTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // On permission grant, refresh weather
        val repo = WeatherRepository.getInstance(this)
        lifecycleScopeLaunch {
            repo.refreshWeather()
        }
    }

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            block()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }

        setContent {
            NothingRainTheme {
                WeatherDashboardScreen(
                    repository = WeatherRepository.getInstance(this)
                )
            }
        }
    }
}

@Composable
fun WeatherDashboardScreen(repository: WeatherRepository) {
    val summary by repository.currentSummary.collectAsState()
    val radarFrames by repository.radarFrames.collectAsState()
    val radarHost by repository.radarHost.collectAsState()
    val location by repository.currentLocation.collectAsState()

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (summary == null) {
            isRefreshing = true
            repository.refreshWeather()
            isRefreshing = false
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("condense_prefs", android.content.Context.MODE_PRIVATE) }
    var isMeteoMode by remember { mutableStateOf(prefs.getBoolean("is_meteo_mode", false)) }

    Scaffold(
        containerColor = Color(0xFF000000)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFD71920), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (summary?.locationName ?: "WEATHER").uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mode Selector: CORE (Essential) vs METEO (Detailed)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isMeteoMode) Color(0xFFD71920) else Color.Transparent)
                                .clickable {
                                    isMeteoMode = false
                                    prefs.edit().putBoolean("is_meteo_mode", false).apply()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CORE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isMeteoMode) Color(0xFFD71920) else Color.Transparent)
                                .clickable {
                                    isMeteoMode = true
                                    prefs.edit().putBoolean("is_meteo_mode", true).apply()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "METEO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                repository.refreshWeather()
                                isRefreshing = false
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFFD71920),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (summary != null) {
                val data = summary!!

                // Hero Temperature & Condition
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${data.currentTemp}°",
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = "${data.conditionEmoji} ${data.conditionDescription}",
                            color = Color(0xFFE5E5EA),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "H:${data.todayHigh}°  L:${data.todayLow}°",
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Feels like ${data.feelsLike}°",
                            color = Color(0xFF636366),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Next Rain Card
                RainCountdownCard(data)

                Spacer(modifier = Modifier.height(16.dp))

                // UV Index Card
                UvIndexCard(data)

                // Detailed Meteorologist Telemetry Modules (When in METEO mode)
                if (isMeteoMode && data.meteoTelemetry != null) {
                    val meteo = data.meteoTelemetry

                    Spacer(modifier = Modifier.height(16.dp))
                    AirQualityMeteoCard(meteo)

                    Spacer(modifier = Modifier.height(16.dp))
                    WindBarometerMeteoCard(meteo)

                    Spacer(modifier = Modifier.height(16.dp))
                    SolarArcMeteoCard(meteo)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Hourly Forecast Row
                Text(
                    text = "HOURLY FORECAST",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(data.hourlyForecast) { item ->
                        HourlyForecastItemView(item)
                    }
                }

                // 14-Day Daily Forecast Row
                if (data.dailyForecast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "14-DAY DAILY FORECAST",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(data.dailyForecast) { item ->
                            DailyForecastItemView(item)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Live Interactive Radar Map Section
                Text(
                    text = "LIVE DOPPLER RADAR MAP",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                RadarMapView(
                    latitude = location.first,
                    longitude = location.second,
                    radarFrames = radarFrames,
                    radarHost = radarHost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD71920))
                }
            }
        }
    }
}

@Composable
fun RainCountdownCard(data: WeatherSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isRainingNow) "🌧️ PRECIPITATION" else "☔ NEXT PRECIPITATION",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = if (data.isRainingNow) "ACTIVE" else "UPCOMING",
                    color = if (data.isRainingNow) Color(0xFFFF453A) else Color(0xFF0A84FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = data.nextRainHeadline,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = data.nextRainSubtext,
                color = Color(0xFFA1A1A6),
                fontSize = 14.sp
            )

            // 7-Day Rainfall Trend Line Chart
            if (data.dailyForecast.isNotEmpty()) {
                SevenDayRainTrendChart(data.dailyForecast)
            }
        }
    }
}

@Composable
fun SevenDayRainTrendChart(dailyList: List<DailyItem>) {
    val days = remember(dailyList) { dailyList.take(7) }
    if (days.isEmpty()) return

    val maxRain = remember(days) { (days.maxOfOrNull { it.precipSum } ?: 0.5).coerceAtLeast(0.25) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-DAY RAINFALL (INCHES)",
                color = Color(0xFF8E8E93),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            val totalRain = remember(days) { String.format(java.util.Locale.US, "%.2f\"", days.sumOf { it.precipSum }) }
            Text(
                text = "Total: $totalRain",
                color = Color(0xFF0A84FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Data points value row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { item ->
                Text(
                    text = if (item.precipSum > 0.0) String.format(java.util.Locale.US, "%.1f\"", item.precipSum) else "0\"",
                    color = if (item.precipSum > 0.0) Color(0xFF0A84FF) else Color(0xFF636366),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Canvas Line Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (days.size - 1).coerceAtLeast(1)

            val points = days.mapIndexed { index, item ->
                val x = index * stepX
                val normalizedY = (item.precipSum / maxRain).coerceIn(0.0, 1.0)
                val y = height - (normalizedY * (height - 12f) + 6f).toFloat()
                Offset(x, y)
            }

            // Fill gradient area
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x400A84FF), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            drawPath(
                path = strokePath,
                color = Color(0xFF0A84FF),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data dots
            points.forEachIndexed { i, pt ->
                val isRain = days[i].precipSum > 0.0
                drawCircle(
                    color = if (isRain) Color(0xFF0A84FF) else Color(0xFF3A3A3C),
                    radius = if (isRain) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = pt
                )
                if (isRain) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { item ->
                Text(
                    text = item.dayLabel.take(3),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun UvIndexCard(data: WeatherSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UV INDEX & SUN EXPOSURE",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = data.uvCategory.uppercase(),
                    color = getUvColor(data.currentUv),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${data.currentUv.toInt()}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Max today: ${data.maxUvToday.toInt()} (${RainEngine.getUvCategory(data.maxUvToday)})",
                    color = Color(0xFFA1A1A6),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (data.currentUv / 12.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = getUvColor(data.currentUv),
                trackColor = Color(0xFF2C2C2E),
            )

            // 7-Day UV Trend Line Chart
            if (data.dailyForecast.isNotEmpty()) {
                SevenDayUvTrendChart(data.dailyForecast)
            }
        }
    }
}

@Composable
fun SevenDayUvTrendChart(dailyList: List<DailyItem>) {
    val days = remember(dailyList) { dailyList.take(7) }
    if (days.isEmpty()) return

    val maxUv = remember(days) { (days.maxOfOrNull { it.maxUv } ?: 10.0).coerceAtLeast(8.0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-DAY UV INDEX TREND",
                color = Color(0xFF8E8E93),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            val peakUv = remember(days) { days.maxOfOrNull { it.maxUv }?.toInt() ?: 0 }
            Text(
                text = "Peak: $peakUv (${RainEngine.getUvCategory(peakUv.toDouble())})",
                color = Color(0xFFFF9F0A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Data points value row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { item ->
                Text(
                    text = "${item.maxUv.toInt()}",
                    color = getUvColor(item.maxUv),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Canvas Line Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (days.size - 1).coerceAtLeast(1)

            val points = days.mapIndexed { index, item ->
                val x = index * stepX
                val normalizedY = (item.maxUv / maxUv).coerceIn(0.0, 1.0)
                val y = height - (normalizedY * (height - 12f) + 6f).toFloat()
                Offset(x, y)
            }

            // Fill gradient area
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x40FF9F0A), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            drawPath(
                path = strokePath,
                color = Color(0xFFFF9F0A),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data dots
            points.forEachIndexed { i, pt ->
                val uvVal = days[i].maxUv
                drawCircle(
                    color = getUvColor(uvVal),
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { item ->
                Text(
                    text = item.dayLabel.take(3),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HourlyForecastItemView(item: HourlyItem) {
    Column(
        modifier = Modifier
            .background(Color(0xFF141416), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.timeLabel,
            color = Color(0xFF8E8E93),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.emoji,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.temp}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (item.precipProb > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.precipProb}%",
                color = Color(0xFF0A84FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DailyForecastItemView(item: DailyItem) {
    Column(
        modifier = Modifier
            .background(Color(0xFF141416), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.dayLabel.uppercase(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = item.dateLabel,
            color = Color(0xFF8E8E93),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.emoji,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.maxTemp}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${item.minTemp}°",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp
        )
        if (item.precipProb > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.precipProb}%",
                color = Color(0xFF0A84FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getUvColor(uv: Double): Color {
    return when {
        uv < 3.0 -> Color(0xFF30D158) // Green
        uv < 6.0 -> Color(0xFFFFD60A) // Yellow
        uv < 8.0 -> Color(0xFFFF9F0A) // Orange
        uv < 11.0 -> Color(0xFFFF453A) // Red
        else -> Color(0xFFBF5AF2) // Purple
    }
}

@Composable
fun AirQualityMeteoCard(telemetry: MeteoTelemetry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🍃 AIR QUALITY & PARTICULATES",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = telemetry.aqiCategory.uppercase(),
                    color = when {
                        telemetry.aqi <= 50 -> Color(0xFF34C759)
                        telemetry.aqi <= 100 -> Color(0xFFFFCC00)
                        telemetry.aqi <= 150 -> Color(0xFFFF9500)
                        else -> Color(0xFFFF3B30)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${telemetry.aqi}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "US AQI · PM2.5: ${telemetry.pm25} µg/m³ · PM10: ${telemetry.pm10}",
                    color = Color(0xFFA1A1A6),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (telemetry.aqi / 200.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    telemetry.aqi <= 50 -> Color(0xFF34C759)
                    telemetry.aqi <= 100 -> Color(0xFFFFCC00)
                    telemetry.aqi <= 150 -> Color(0xFFFF9500)
                    else -> Color(0xFFFF3B30)
                },
                trackColor = Color(0xFF2C2C2E)
            )
        }
    }
}

@Composable
fun WindBarometerMeteoCard(telemetry: MeteoTelemetry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💨 WIND & BAROMETER TENDENCY",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${telemetry.windDirectionCardinal} ${telemetry.windSpeedMph} MPH",
                    color = Color(0xFF0A84FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "SUSTAINED / GUSTS", color = Color(0xFF636366), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${telemetry.windSpeedMph} mph (Gust ${telemetry.windGustsMph})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "BAROMETER", color = Color(0xFF636366), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${telemetry.pressureHpa} hPa", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DEW POINT", color = Color(0xFF636366), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${telemetry.dewPoint}°", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "HUMIDITY / CLOUDS", color = Color(0xFF636366), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${telemetry.humidity}% · ${telemetry.cloudCoverPercent}% Cloud", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SolarArcMeteoCard(telemetry: MeteoTelemetry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🌅 SOLAR ARC & DAYLIGHT",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = telemetry.daylightLeftStr.uppercase(),
                    color = Color(0xFFFF9F0A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = telemetry.sunriseStr, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Text(text = "───────●───────", color = Color(0xFFFF9F0A), fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌇", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = telemetry.sunsetStr, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "✨ Golden hour begins at ${telemetry.goldenHourStr}",
                color = Color(0xFFA1A1A6),
                fontSize = 13.sp
            )
        }
    }
}
