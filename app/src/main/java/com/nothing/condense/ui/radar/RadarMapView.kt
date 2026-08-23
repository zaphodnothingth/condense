package com.nothing.condense.ui.radar

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.MotionEvent
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
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
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
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
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val overlaysList = remember { mutableListOf<TilesOverlay>() }

    // Loop animation through available radar frames
    LaunchedEffect(radarFrames.size, isPlaying) {
        if (radarFrames.isNotEmpty() && isPlaying) {
            while (true) {
                delay(650)
                currentFrameIndex = (currentFrameIndex + 1) % radarFrames.size
            }
        }
    }

    // Toggle overlay visibility seamlessly without rebuilding overlays
    LaunchedEffect(currentFrameIndex, overlaysList.size) {
        overlaysList.forEachIndexed { index, overlay ->
            overlay.isEnabled = (index == currentFrameIndex)
        }
        mapViewRef.value?.postInvalidate()
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef.value?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141416))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setDestroyMode(false)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    setBackgroundColor(AndroidColor.parseColor("#141416"))

                    // 1. Dark CartoDB Base Map Tile Source
                    val darkTileSource = XYTileSource(
                        "CartoDark",
                        0,
                        19,
                        256,
                        ".png",
                        arrayOf(
                            "https://a.basemaps.cartocdn.com/dark_all/",
                            "https://b.basemaps.cartocdn.com/dark_all/",
                            "https://c.basemaps.cartocdn.com/dark_all/"
                        )
                    )
                    setTileSource(darkTileSource)

                    // 2. User Location Marker (Red dot)
                    val centerPoint = GeoPoint(latitude, longitude)
                    val userMarker = Marker(this).apply {
                        position = centerPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        val dot = ShapeDrawable(OvalShape()).apply {
                            intrinsicWidth = 32
                            intrinsicHeight = 32
                            paint.color = AndroidColor.parseColor("#D71920")
                            paint.style = Paint.Style.FILL
                        }
                        icon = dot
                    }
                    overlays.add(userMarker)

                    controller.setZoom(7.0)
                    controller.setCenter(centerPoint)

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                mapViewRef.value = mapView
                if (radarFrames.isNotEmpty() && (overlaysList.isEmpty() || overlaysList.size != radarFrames.size)) {
                    overlaysList.forEach { mapView.overlays.remove(it) }
                    overlaysList.clear()

                    radarFrames.forEachIndexed { index, frame ->
                        val frameSource = XYTileSource(
                            "Rain_${frame.time}",
                            0,
                            20,
                            256,
                            "/6/1_1.png",
                            arrayOf("$radarHost${frame.path}/256/")
                        )
                        val provider = MapTileProviderBasic(mapView.context, frameSource)
                        val overlay = TilesOverlay(provider, mapView.context).apply {
                            loadingBackgroundColor = AndroidColor.TRANSPARENT
                            loadingLineColor = AndroidColor.TRANSPARENT
                            isEnabled = (index == currentFrameIndex)
                        }
                        mapView.overlays.add(0, overlay)
                        overlaysList.add(overlay)
                    }
                    mapView.postInvalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Radar Playback Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .background(
                    color = Color(0xE6141416),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
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
                            .size(34.dp)
                            .background(Color(0xFFD71920), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "LIVE DOPPLER RADAR",
                            color = Color(0xFF8E8E93),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        val frame = radarFrames.getOrNull(currentFrameIndex)
                        val timeFormatted = remember(frame) {
                            if (frame != null) {
                                val instant = Instant.ofEpochSecond(frame.time)
                                val local = instant.atZone(ZoneId.systemDefault())
                                local.format(DateTimeFormatter.ofPattern("h:mm a"))
                            } else {
                                "Live"
                            }
                        }
                        Text(
                            text = timeFormatted,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = {
                        mapViewRef.value?.controller?.animateTo(GeoPoint(latitude, longitude), 8.5, 500L)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF2C2C2E), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
