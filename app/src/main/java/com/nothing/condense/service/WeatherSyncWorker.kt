package com.nothing.condense.service

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nothing.condense.data.WeatherRepository
import com.nothing.condense.widget.NothingRainWidget1x1
import com.nothing.condense.widget.NothingRainWidget1x2
import com.nothing.condense.widget.NothingRainWidget2x1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repo = WeatherRepository.getInstance(context)
            repo.refreshWeather()

            // Update all active glance widgets
            NothingRainWidget2x1().updateAll(context)
            NothingRainWidget1x2().updateAll(context)
            NothingRainWidget1x1().updateAll(context)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
