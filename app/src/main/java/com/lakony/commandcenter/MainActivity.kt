package com.lakony.commandcenter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lakony.commandcenter.notifications.TaskNotificationScheduler
import com.lakony.commandcenter.smith.SmithDailyScheduler
import com.lakony.commandcenter.ui.CommandCenterRoot
import com.lakony.commandcenter.ui.LakonyTheme
import com.lakony.commandcenter.ui.ThemeController

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.initialize(this)
        TaskNotificationScheduler.createChannel(this)
        TaskNotificationScheduler.rescheduleCached(this)
        SmithDailyScheduler.initialize(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LakonyTheme {
                CommandCenterRoot()
            }
        }
    }
}
