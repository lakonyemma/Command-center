package com.lakony.commandcenter.smith

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lakony.commandcenter.MainActivity
import com.lakony.commandcenter.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object SmithDailyScheduler {
    private const val CHANNEL_ID = "smith_daily_briefs"
    private const val MORNING_REQUEST = 8101
    private const val EVENING_REQUEST = 8102
    private const val TYPE_MORNING = "morning"
    private const val TYPE_EVENING = "evening"

    fun initialize(context: Context) {
        createChannel(context)
        scheduleNext(context, TYPE_MORNING)
        scheduleNext(context, TYPE_EVENING)
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
        val channel = NotificationChannel(CHANNEL_ID, "Smith daily briefings", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Proactive morning and evening business briefings from Smith"
            enableVibration(true)
            setSound(sound, audio)
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleNext(context: Context, type: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        val targetTime = if (type == TYPE_EVENING) LocalTime.of(19, 0) else LocalTime.of(8, 0)
        var target = LocalDateTime.of(LocalDate.now(), targetTime)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val triggerAt = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = pendingIntent(context, type)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(context: Context, type: String): PendingIntent {
        val requestCode = if (type == TYPE_EVENING) EVENING_REQUEST else MORNING_REQUEST
        val intent = Intent(context, SmithBriefReceiver::class.java).putExtra("brief_type", type)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun nextType(type: String): String = if (type == TYPE_EVENING) TYPE_EVENING else TYPE_MORNING
}

class SmithBriefReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("brief_type").orEmpty().ifBlank { "morning" }
        val engine = SmithRevenueEngine(context)
        val brief = if (type == "evening") engine.eveningBrief() else engine.morningBrief()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            val openApp = PendingIntent.getActivity(
                context,
                type.hashCode(),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val body = brief.priorities.joinToString(" • ")
            val notification = NotificationCompat.Builder(context, "smith_daily_briefs")
                .setSmallIcon(R.drawable.command_center_icon)
                .setContentTitle(if (type == "evening") "Smith evening review" else "Smith morning brief")
                .setContentText(brief.headline)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${brief.headline}\n\n$body"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .build()
            NotificationManagerCompat.from(context).notify(type.hashCode(), notification)
        }

        SmithDailyScheduler.scheduleNext(context, SmithDailyScheduler.nextType(type))
    }
}

class SmithBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            SmithDailyScheduler.initialize(context)
        }
    }
}
