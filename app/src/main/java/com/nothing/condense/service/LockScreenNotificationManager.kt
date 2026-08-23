package com.nothing.condense.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nothing.condense.R
import com.nothing.condense.CondenseApp
import com.nothing.condense.data.model.WeatherSummary
import com.nothing.condense.ui.MainActivity

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

        val notification = NotificationCompat.Builder(context, CondenseApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(CondenseApp.NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission check fallback
        }
    }
}
