# Condense (Nothing Edition) 🌧️☀️

An ultra-compact, high-density weather app, widget suite, and Quick Settings integration engineered specifically for **Nothing Phone** and **Nothing OS 2.x / 3.x**.

---

## 🎯 Design Philosophy: "Compact & Informative"

In true **Nothing OS** spirit, Condense rejects bloated multi-screen weather interfaces, oversized illustrations, and useless whitespace. Every element is designed around three strict principles:

1. **Information Density without Clutter**: Maximum telemetry (Temp, Range, UV, Rain Nowcasting, Accumulation) presented in clean, scannable, monospace-aligned layouts.
2. **Zero-Wait Glances**: Critical weather vectors (next precipitation window, peak UV, 7-day rainfall) must be readable in under half a second without waiting for text to scroll or marquee.
3. **Nothing Aesthetic Alignment**: Deep OLED black backgrounds (`#000000`, `#141416`), signature **Nothing Red** (`#D71920`) accent nodes, Nd-dot typography cues, and native dark-matter CartoDB mapping.

---

## 📱 Current Feature Suite

### 1. "Time to Next Rain" Engine & 7-Day Rainfall Trend
- **Ensemble Precision**: Powered by the **Open-Meteo High-Resolution Ensemble API** + **RainViewer Live Radar**.
- **Smart Countdown**:
  - `🌧️ Raining Now`
  - `☔ Rain in 45m` (85% probability)
  - `☔ Rain in 4 days` (Wed 3 PM · 28%)
  - `☀️ No rain expected` (Clear for 7+ days)
- **7-Day Rainfall Trend Line Chart (Inches)**:
  - Daily rainfall amounts in inches (`0"`, `1.4"`, `0.8"`) plotted on a smooth blue bezier curve with area gradient fill.
  - Cumulative 7-day rainfall total header (`Total: 2.19"`).

### 2. UV Index & 7-Day Peak Sun Exposure
- **Live UV Telemetry**: Current UV index + today's maximum level with risk categories (`Low`, `Moderate`, `High`, `Very High`, `Extreme`).
- **7-Day UV Peak Line Chart**:
  - Forecasted peak daily UV index with sun-gradient glow and category-coded data nodes.
  - Peak week risk indicator (`Peak: 7 (High)`).

### 3. Interactive Doppler Radar Map with Time Scrubber
- **Hardware-Accelerated Dark Map**: Native CartoDB Dark Matter base tiles scaled up to zoom level 22.
- **NEXRAD Color Scheme**: Multi-intensity Doppler precipitation echoes (green $\rightarrow$ yellow $\rightarrow$ orange $\rightarrow$ red $\rightarrow$ magenta) over a transparent canvas.
- **Interactive Time Scrubber Bar**: Drag the Nothing Red slider thumb back and forth across 2+ hours of radar frames to inspect storm cells minute-by-minute with automatic pause-on-drag and live timestamp readout.
- **Zero-Flicker Architecture**: Pre-allocated memory overlay pool with instant `overlay.isEnabled` toggling.

### 4. Compact Widgets Suite (Nothing OS Native)
- **1x2 Vertical Pill**:
  ```text
  ┌────────┐
  │ ⛅ 77° │
  │ 84/69  │
  │ 0 UV 7 │
  │ 🌧️ 5d  │
  └────────┘
  ```
- **2x1 Horizontal Pill**:
  ```text
  ┌─────────────────────────┐
  │ ⛅ 77°     │    84/69    │
  │ 0 UV 7     │    🌧️ 5d    │
  └─────────────────────────┘
  ```
- **1x1 Minimal Glyph**: Circular / square quick glance with current temp, condition emoji, and rain status dot.

### 5. Quick Settings Tile (`CondenseTileService`)
- Formatted specifically for Nothing OS expanded tiles and Lock Screen / Home Screen Quick Settings widgets:
  ```text
  ┌────────────────────────┐
  │ ⛅ 77° - 84/69          │
  │ 0 UV 7 - 🌧️ 5d        │
  └────────────────────────┘
  ```
- Instant background synchronization and one-tap app launch.

### 6. Pinned High-Priority Notification
- Pinned persistent notification channel with `PRIORITY_MAX` and `IMPORTANCE_HIGH` keeping live weather and rain countdowns anchored at the top of the notification shade.

---

## 🔭 Feature Exploration & Module Roadmap

Keeping with the **"Compact & Informative"** philosophy, here are high-impact modules and capabilities ready for exploration:

### 💨 Module A: Air Quality (AQI) & Pollen Glance
- **Data Source**: Open-Meteo Air Quality API (free, real-time).
- **Compact UI**: Ultra-compact 1-row telemetry card:
  `🍃 AQI 24 (Good) · PM2.5: 6 µg/m³ · Tree Pollen: Low`
- **Widget Integration**: Optional toggle on 2x1 pill or dedicated 1x1 AQI tile.

### 🧭 Module B: Wind Vectors & Barometric Pressure Tendency
- **Data Source**: Open-Meteo Wind Speed, Direction, Gusts & Surface Pressure.
- **Compact UI**:
  `💨 12 mph NE (Gust 22) · 1016 hPa ↗ (Rising / Clearing)`
- **Barometer Forecast Value**: Rapidly falling pressure serves as an instant physical indicator of approaching storm fronts.

### 🌅 Module C: Solar Arc & Golden Hour Countdown
- **Compact UI**: Sleek dot-matrix daylight progress bar:
  `🌅 6:24 AM ──●───────── 🌇 7:51 PM (2h 15m daylight left · Golden hour 7:15 PM)`

### 🔴 Module D: Nothing Glyph Interface Ambient Alert
- **Nothing Phone Glyph Integration**: Trigger a subtle, non-intrusive Glyph pulse or Glyph progress bar countdown when rain is detected within $< 15$ minutes.

### 🗺️ Module E: Mini Radar Home Screen Widget (2x2)
- A compact 2x2 home screen widget rendering a mini live Doppler radar tile loop centered on your exact coordinates.

---

## 🛠️ Tech Stack & Architecture

- **Language & Framework**: Kotlin 1.9+, Jetpack Compose, Compose Glance (AppWidgets)
- **Map & Radar**: osmdroid (OpenStreetMap), CartoDB Dark Matter Tiles, RainViewer API v2
- **Weather Telemetry**: Open-Meteo Ensemble Weather API (ECMWF IFS, NOAA HRRR, DWD ICON)
- **Networking**: Ktor Client with OkHttp engine & Kotlinx Serialization
- **Background Tasks**: AndroidX WorkManager, Foreground Service, Android Quick Settings TileService

---

## 🚀 Installation & Deployment

```powershell
# Set Java 21 Home
$env:JAVA_HOME = "C:\Users\steve\.jdks\jbr-21.0.11"

# Build Debug APK
.\gradlew.bat assembleDebug

# Install to connected Nothing Phone
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
