package com.lakony.commandcenter.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.data.AppSettingsStore
import com.lakony.commandcenter.notifications.TaskNotificationScheduler
import com.lakony.commandcenter.profile.ProfileImageStore

@Composable
fun EnhancedSettingsScreen(
    displayName: String,
    onSaveName: (String) -> Unit,
    tasklyConnected: Boolean,
    tasklyUser: String,
    onOpenTaskly: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppSettingsStore(context) }
    val profileStore = remember { ProfileImageStore(context) }
    var name by remember(displayName) { mutableStateOf(displayName) }
    var saved by remember { mutableStateOf(false) }
    var profileImage by remember { mutableStateOf(profileStore.load()) }
    var notificationsEnabled by remember { mutableStateOf(settings.tasklyDueNotifications) }
    val exactAlarmAllowed = TaskNotificationScheduler.canScheduleExact(context)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && profileStore.saveFrom(uri)) profileImage = profileStore.load()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium)
        Text("Swiss grid. Bauhaus geometry. Your colors.", color = MutedText)

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PROFILE", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (profileImage != null) {
                        Image(
                            bitmap = profileImage!!,
                            contentDescription = "Profile picture",
                            modifier = Modifier.size(76.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.size(76.dp).clip(CircleShape).background(SoftBlue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { imagePicker.launch("image/*") }) { Text("Choose picture") }
                        if (profileImage != null) {
                            TextButton(onClick = { profileStore.remove(); profileImage = null }) { Text("Remove picture") }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onSaveName(name.trim().ifBlank { "Lakony" })
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save profile") }
                if (saved) Text("Profile saved on this phone.", color = PrimaryBlue)
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("COLOR THEME", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                AppThemeStyle.entries.forEach { style ->
                    FilterChip(
                        selected = ThemeController.style == style,
                        onClick = { ThemeController.set(context, style) },
                        label = { Text(style.label) },
                    )
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TASKLY REMINDERS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Due-time sound + vibration", fontWeight = FontWeight.Bold)
                        Text("Alerts when a synced Taskly task reaches its due time.", color = MutedText, fontSize = 12.sp)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            settings.tasklyDueNotifications = enabled
                            if (!enabled) TaskNotificationScheduler.cancelAll(context)
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
                if (!exactAlarmAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }) { Text("Allow exact due-time alarms") }
                    Text("Without this Android may deliver the reminder slightly late.", color = MutedText, fontSize = 11.sp)
                } else {
                    Text("Exact due-time alarms enabled.", color = PrimaryBlue, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, TaskNotificationScheduler.CHANNEL_ID)
                        })
                    }
                }) { Text("Open sound & vibration settings") }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TASKLY", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(if (tasklyConnected) "Connected as ${tasklyUser.ifBlank { "Taskly user" }}" else "Not connected", color = MutedText)
                OutlinedButton(onClick = onOpenTaskly) { Text(if (tasklyConnected) "Manage Taskly" else "Connect Taskly") }
            }
        }

        Text("Task management: Taskly  •  Version 1.2", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))
    }
}
