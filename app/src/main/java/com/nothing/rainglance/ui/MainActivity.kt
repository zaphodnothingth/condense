package com.nothing.rainglance.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nothing.rainglance.data.RainEngine
import com.nothing.rainglance.data.WeatherRepository
import com.nothing.rainglance.data.model.HourlyItem
import com.nothing.rainglance.data.model.WeatherSummary
import com.nothing.rainglance.ui.radar.RadarMapView
import com.nothing.rainglance.ui.theme.NothingRainTheme
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

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
                            modifier = Modifier.size(20.dp),
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

fun getUvColor(uv: Double): Color {
    return when {
        uv < 3.0 -> Color(0xFF30D158) // Green
        uv < 6.0 -> Color(0xFFFFD60A) // Yellow
        uv < 8.0 -> Color(0xFFFF9F0A) // Orange
        uv < 11.0 -> Color(0xFFFF453A) // Red
        else -> Color(0xFFBF5AF2) // Purple
    }
}
