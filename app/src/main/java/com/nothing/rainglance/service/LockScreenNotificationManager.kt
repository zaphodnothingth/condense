package com.nothing.rainglance.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nothing.rainglance.R
import com.nothing.rainglance.RainGlanceApp
import com.nothing.rainglance.data.model.WeatherSummary
import com.nothing.rainglance.ui.MainActivity

object LockScreenNotificationManager {

    fun updateNotification(context: Context, summary: WeatherSummary?) {
        if (summary == null) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${summary.currentTemp}° ${summary.conditionEmoji} · H:${summary.todayHigh}° L:${summary.todayLow}° · UV ${summary.currentUv.toInt()}"
        val content = if (summary.isRainingNow) {
            "🌧️ ${summary.nextRainHeadline} - ${summary.nextRainSubtext}"
        } else {
            "☔ Next Rain: ${summary.nextRainHeadline} (${summary.nextRainSubtext})"
        }

        val notification = NotificationCompat.Builder(context, RainGlanceApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(RainGlanceApp.NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission check fallback
        }
    }
}
