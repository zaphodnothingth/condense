package com.nothing.condense.ui.radar

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nothing.condense.data.model.RadarFrame
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RadarMapView(
    latitude: Double,
    longitude: Double,
    radarFrames: List<RadarFrame>,
    radarHost: String,
    modifier: Modifier = Modifier
) {
    val framesJson = remember(radarFrames) {
        val array = JSONArray()
        radarFrames.forEach { frame ->
            val obj = JSONObject()
            obj.put("time", frame.time)
            obj.put("path", frame.path)
            array.put(obj)
        }
        array.toString()
    }

    val html = remember(latitude, longitude, framesJson, radarHost) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #121214; }
                .leaflet-control-attribution { display: none !important; }
                .user-marker {
                    width: 14px;
                    height: 14px;
                    background: #D71920;
                    border: 2.5px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 10px rgba(215, 25, 32, 0.8);
                }
                .radar-controls {
                    position: absolute;
                    bottom: 12px;
                    left: 12px;
                    right: 12px;
                    z-index: 1000;
                    background: rgba(20, 20, 22, 0.92);
                    backdrop-filter: blur(10px);
                    border: 1px solid #2C2C2E;
                    border-radius: 16px;
                    padding: 8px 14px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    color: white;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                }
                .btn {
                    background: #D71920;
                    border: none;
                    border-radius: 50%;
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 14px;
                    cursor: pointer;
                }
                .time-label {
                    font-size: 13px;
                    font-weight: bold;
                    margin-left: 10px;
                }
                .sub-label {
                    font-size: 10px;
                    color: #8E8E93;
                    letter-spacing: 0.5px;
                    text-transform: uppercase;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div class="radar-controls">
                <div style="display: flex; align-items: center;">
                    <button class="btn" id="playBtn" onclick="togglePlay()">⏸</button>
                    <div style="margin-left: 10px;">
                        <div class="sub-label">Live Doppler Radar</div>
                        <div class="time-label" id="timeLabel">Loading...</div>
                    </div>
                </div>
                <button class="btn" style="background: #2C2C2E;" onclick="recenter()">📍</button>
            </div>

            <script>
                const lat = $latitude;
                const lon = $longitude;
                const host = "$radarHost";
                const frames = $framesJson;

                const map = L.map('map', {
                    center: [lat, lon],
                    zoom: 8,
                    zoomControl: false,
                    attributionControl: false
                });

                // Smooth dark base map tiles
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                // User Location Pulse Dot
                const userIcon = L.divIcon({
                    className: 'user-marker',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });
                L.marker([lat, lon], { icon: userIcon }).addTo(map);

                // Pre-cache radar layers
                const radarLayers = [];
                frames.forEach(frame => {
                    const layer = L.tileLayer(host + frame.path + '/256/{z}/{x}/{y}/2/1_1.png', {
                        opacity: 0,
                        zIndex: 100,
                        maxZoom: 19,
                        maxNativeZoom: 12
                    });
                    layer.addTo(map);
                    radarLayers.push({ layer: layer, time: frame.time });
                });

                let currentIndex = 0;
                let isPlaying = true;
                let timer = null;

                function showFrame(index) {
                    if (!radarLayers.length) return;
                    radarLayers.forEach((item, i) => {
                        item.layer.setOpacity(i === index ? 0.75 : 0);
                    });
                    const date = new Date(radarLayers[index].time * 1000);
                    document.getElementById('timeLabel').innerText = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                }

                function step() {
                    if (!isPlaying || !radarLayers.length) return;
                    currentIndex = (currentIndex + 1) % radarLayers.length;
                    showFrame(currentIndex);
                }

                if (radarLayers.length > 0) {
                    currentIndex = radarLayers.length - 1;
                    showFrame(currentIndex);
                    timer = setInterval(step, 650);
                }

                function togglePlay() {
                    isPlaying = !isPlaying;
                    document.getElementById('playBtn').innerText = isPlaying ? '⏸' : '▶';
                }

                function recenter() {
                    map.setView([lat, lon], 8);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141416))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    setBackgroundColor(AndroidColor.parseColor("#141416"))
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://tilecache.rainviewer.com", html, "text/html", "UTF-8", null)
                }
            },
            update = { view ->
                view.loadDataWithBaseURL("https://tilecache.rainviewer.com", html, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
