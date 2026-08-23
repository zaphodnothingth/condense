package com.nothing.condense.ui.radar

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nothing.condense.data.model.RadarFrame
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RadarMapView(
    latitude: Double,
    longitude: Double,
    radarFrames: List<RadarFrame>,
    radarHost: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var radarOverlay by remember { mutableStateOf<TilesOverlay?>(null) }

    // Loop animation when playing
    LaunchedEffect(isPlaying, radarFrames.size) {
        if (radarFrames.isNotEmpty() && isPlaying) {
            while (true) {
                delay(650)
                currentFrameIndex = (currentFrameIndex + 1) % radarFrames.size
            }
        }
    }

    // Update overlay when frame changes
    LaunchedEffect(currentFrameIndex, radarFrames, radarHost) {
        if (radarFrames.isNotEmpty() && currentFrameIndex < radarFrames.size && mapViewInstance != null) {
            val frame = radarFrames[currentFrameIndex]
            mapViewInstance?.let { map ->
                radarOverlay?.let { map.overlays.remove(it) }

                val tileSource = object : OnlineTileSourceBase(
                    "RainViewer-${frame.time}",
                    0, 12, 256, ".png",
                    arrayOf(radarHost)
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val zoom = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "$baseUrl${frame.path}/256/$zoom/$x/$y/2/1_1.png"
                    }
                }

                val provider = MapTileProviderBasic(context, tileSource)
                val newOverlay = TilesOverlay(provider, context).apply {
                    loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                }
                map.overlays.add(newOverlay)
                radarOverlay = newOverlay
                map.invalidate()
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF141416), RoundedCornerShape(24.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 12.0
                    controller.setZoom(8.0)
                    controller.setCenter(GeoPoint(latitude, longitude))

                    // Allow panning without triggering parent vertical scroll
                    setOnTouchListener { v, event ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    // Location overlay
                    val locationOverlay = MyLocationNewOverlay(this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)

                    mapViewInstance = this
                }
            },
            update = { map ->
                map.controller.setCenter(GeoPoint(latitude, longitude))
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                mapViewInstance?.onDetach()
            }
        }

        // Radar Player Controls Overlay
        if (radarFrames.isNotEmpty()) {
            val currentFrame = radarFrames.getOrNull(currentFrameIndex)
            val timeLabel = currentFrame?.let {
                val instant = Instant.ofEpochSecond(it.time)
                DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(instant)
            } ?: "--:--"

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xE6141416), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD71920), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "LIVE PRECIPITATION RADAR",
                                color = Color(0xFF8E8E93),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = timeLabel,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            mapViewInstance?.controller?.animateTo(GeoPoint(latitude, longitude))
                            mapViewInstance?.controller?.setZoom(8.5)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = currentFrameIndex.toFloat(),
                    onValueChange = {
                        isPlaying = false
                        currentFrameIndex = it.toInt().coerceIn(0, radarFrames.size - 1)
                    },
                    valueRange = 0f..(radarFrames.size - 1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD71920),
                        activeTrackColor = Color(0xFFD71920),
                        inactiveTrackColor = Color(0xFF3A3A3C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
