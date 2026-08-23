package com.nothing.condense

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nothing.condense.service.WeatherSyncWorker
import org.osmdroid.config.Configuration
import java.util.concurrent.TimeUnit

class CondenseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid user agent for tile caching
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        createNotificationChannel()
        setupBackgroundSync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Condense Weather & Rain Live",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pinned live weather and rain radar updates"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            20, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val CHANNEL_ID = "condense_weather_channel_v2"
        const val NOTIFICATION_ID = 1001
    }
}
