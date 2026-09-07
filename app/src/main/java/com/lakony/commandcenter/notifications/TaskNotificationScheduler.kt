package com.lakony.commandcenter.notifications

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
import com.lakony.commandcenter.data.AppSettingsStore
import com.lakony.commandcenter.taskly.TasklyTask
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

object TaskNotificationScheduler {
    const val CHANNEL_ID = "taskly_due_tasks"
    private const val PREFS = "taskly_due_alarm_cache"
    private const val KEY_TASKS = "tasks"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
        val channel = NotificationChannel(CHANNEL_ID, "Taskly due tasks", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Sound and vibration reminders when a Taskly task reaches its due time"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 180, 350, 180, 500)
            setSound(sound, audio)
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleAll(context: Context, tasks: List<TasklyTask>) {
        createChannel(context)
        if (!AppSettingsStore(context).tasklyDueNotifications) {
            cancelAll(context)
            return
        }
        val active = tasks.filter { !it.completed && !it.dueDate.isNullOrBlank() }
        cancelStale(context, active.map { it.id }.toSet())
        saveCache(context, active)
        active.forEach { schedule(context, it) }
    }

    fun rescheduleCached(context: Context) {
        if (!AppSettingsStore(context).tasklyDueNotifications) return
        scheduleAll(context, loadCache(context))
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, taskId, "", 0L))
        val remaining = loadCache(context).filterNot { it.id == taskId }
        saveCache(context, remaining)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        loadCache(context).forEach { alarmManager.cancel(pendingIntent(context, it.id, "", 0L)) }
        saveCache(context, emptyList())
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun cancelStale(context: Context, activeIds: Set<String>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        loadCache(context).filterNot { it.id in activeIds }.forEach {
            alarmManager.cancel(pendingIntent(context, it.id, "", 0L))
        }
    }

    private fun schedule(context: Context, task: TasklyTask) {
        val whenMillis = parseDue(task.dueDate) ?: return
        if (whenMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, task.id, task.title, whenMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        }
    }

    private fun pendingIntent(context: Context, taskId: String, title: String, dueAt: Long): PendingIntent {
        val intent = Intent(context, TaskDueReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", title)
            putExtra("due_at", dueAt)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun parseDue(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrNull()
    }

    private fun saveCache(context: Context, tasks: List<TasklyTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("status", task.status)
                put("priority", task.priority)
                put("workspaceId", task.workspaceId)
                put("dueDate", task.dueDate)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private fun loadCache(context: Context): List<TasklyTask> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TASKS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(TasklyTask(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        status = item.optString("status", "TODO"),
                        priority = item.optString("priority", "MEDIUM"),
                        workspaceId = item.optString("workspaceId"),
                        dueDate = item.optString("dueDate").takeIf { it.isNotBlank() && it != "null" },
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }
}

class TaskDueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AppSettingsStore(context).tasklyDueNotifications) return
        TaskNotificationScheduler.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val taskId = intent.getStringExtra("task_id").orEmpty()
        val title = intent.getStringExtra("task_title").orEmpty().ifBlank { "Taskly task" }
        val openApp = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, TaskNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.command_center_icon)
            .setContentTitle("Task due now")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Taskly reminder: $title is due now."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setVibrate(longArrayOf(0, 350, 180, 350, 180, 500))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            TaskNotificationScheduler.rescheduleCached(context)
        }
    }
}
