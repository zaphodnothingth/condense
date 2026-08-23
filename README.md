# Condense (Nothing Edition) 🌧️☀️

A bespoke Android weather app and ultra-compact widget suite engineered specifically for **Nothing Phone 3** and **Nothing OS 2.x / 3.x**.

Built to replicate and surpass the beloved **Apple Watch "Time to Next Rain" complication**, combining high-precision ensemble forecasting with the iconic Nothing monochrome and dot-matrix design language.

---

## 📱 Features

### 1. "Time to Next Rain" Countdown Engine
- **Ensemble Precision**: Powered by **Open-Meteo High-Resolution Ensemble API** (aggregating ECMWF IFS 0.25°, NOAA HRRR, and DWD ICON) + **RainViewer Doppler Radar**.
- **Real-Time Nowcasting**: Scans minute-by-minute and hourly precipitation vectors.
- **Smart Countdown**:
  - `🌧️ Raining Now` (Ongoing precipitation)
  - `☔ Rain in 45m` (85% probability · 0.15 in)
  - `☔ Rain in 3h` (Today at 4 PM · 70%)
  - `☔ Rain tomorrow` (Around 2 PM · 60%)
  - `☀️ No rain expected` (Clear for next 7 days)

### 2. Ultra-Compact Nothing OS Widgets
- **2x1 Nothing Weather Pill**:
  - Top Line: `74° · H:82° L:61° · UV 6 (Max 8)` with signature Nothing Red accent dot.
  - Bottom Line: `🌧️ Next rain: In 3h (80%) · Heavy showers`
  - Compact rounded pill format designed to fit seamlessly into Nothing OS grids.
- **1x1 Minimal Quick Circle**:
  - Displays temperature, rain status dot, and next rain window (`3h`, `Now`, `Dry`).
- **One-Tap Action**: Tapping any widget instantly launches the full app centered directly on your live radar.

### 3. Lock Screen & Always-On Display (AOD)
- **Lock Screen Widget Slot**: Fits into the Nothing OS 4-slot lock screen widget tray.
- **Persistent Low-Priority Status Ticker**: An optional 1-line silent status bar & lockscreen notification that gives you real-time rain countdowns without unlocking your device.

### 4. Interactive Live Doppler Radar Map
- Hardware-accelerated precipitation radar powered by Mapnik + RainViewer tile layers.
- Play/Pause animation controller and scrubber for past & future nowcast frames.
- Auto-centers on your current GPS location.

---

## 🛠️ Project Structure

```
nothing-rain-glance/
├── app/
│   ├── src/main/java/com/nothing/rainglance/
│   │   ├── RainGlanceApp.kt                 # App init, WorkManager & Notification channels
│   │   ├── data/
│   │   │   ├── RainEngine.kt                # Pure algorithmic next-rain & UV calculation
│   │   │   ├── WeatherRepository.kt         # GPS location, API caller & cache
│   │   │   ├── api/WeatherApiService.kt     # Open-Meteo & RainViewer REST client
│   │   │   └── model/WeatherModels.kt       # Forecast & radar models
│   │   ├── widget/
│   │   │   ├── NothingRainWidget2x1.kt       # 2x1 Nothing Pill Glance Widget
│   │   │   ├── NothingRainWidget1x1.kt       # 1x1 Glance Widget
│   │   │   └── NothingRainWidgetReceivers.kt # AppWidget receivers
│   │   ├── service/
│   │   │   ├── LockScreenNotificationManager.kt # Persistent lock screen ticker
│   │   │   └── WeatherSyncWorker.kt         # Background periodic sync (WorkManager)
│   │   └── ui/
│   │       ├── MainActivity.kt              # Main Jetpack Compose dashboard
│   │       ├── radar/RadarMapView.kt        # Interactive Doppler radar map
│   │       └── theme/NothingTheme.kt        # Nothing OS monochrome & red dot palette
│   └── src/test/java/com/nothing/rainglance/
│       └── RainEngineTest.kt                # Unit test suite
```

---

## 🚀 How to Build & Run

### In Android Studio:
1. Open Android Studio.
2. Select **Open** and choose `C:\Users\steve\gits\nothing-rain-glance`.
3. Connect your **Nothing Phone 3** via USB (or wireless debugging) with **Developer Options > USB Debugging** enabled.
4. Click **Run 'app'** (`Shift + F10`).

### On your Nothing Phone 3:
1. Long-press an empty space on your **Home Screen** → tap **Widgets**.
2. Scroll to **RainGlance** → drag the **Nothing Weather Pill (2x1)** or **Nothing Weather (1x1)** widget onto your screen.
3. For the **Lock Screen**: Go to **Settings > Display > Lock Screen > Lock Screen Widgets** and add the RainGlance widget.
