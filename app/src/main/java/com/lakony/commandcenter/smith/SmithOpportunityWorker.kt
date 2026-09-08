package com.lakony.commandcenter.smith

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lakony.commandcenter.MainActivity
import com.lakony.commandcenter.R
import com.lakony.commandcenter.revenue.RevenueApiClient
import com.lakony.commandcenter.revenue.RevenueAuthStore
import java.util.concurrent.TimeUnit

class SmithOpportunityWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = RevenueAuthStore(applicationContext).googleAccessToken()
        if (token.isBlank()) return Result.retry()

        return runCatching {
            val result = RevenueApiClient.scanOpportunities(token)
            val added = result.optInt("added", 0)
            val updated = result.optInt("updated", 0)
            val scanned = result.optInt("scanned", 0)
            if (added > 0) {
                SmithOpportunityScheduler.notifyNewOpportunities(applicationContext, added, scanned)
            } else if (updated > 0) {
                SmithOpportunityScheduler.createChannel(applicationContext)
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}

object SmithOpportunityScheduler {
    private const val UNIQUE_WORK = "smith-opportunity-hunter"
    private const val CHANNEL_ID = "smith_opportunity_hunter"

    fun schedule(context: Context) {
        createChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SmithOpportunityWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Smith opportunities",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when Smith finds new business opportunities"
                enableVibration(true)
            },
        )
    }

    fun notifyNewOpportunities(context: Context, added: Int, scanned: Int) {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = PendingIntent.getActivity(
            context,
            4401,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.command_center_icon)
            .setContentTitle("Smith found $added new opportunity${if (added == 1) "" else "ies"}")
            .setContentText("Scanned $scanned matching listings. Open Mission Control to review them.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Smith found $added new business opportunity${if (added == 1) "" else "ies"} after scanning $scanned matching listings. Nothing has been sent externally; review and approve in Mission Control.",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context).notify(4401, notification)
    }
}
