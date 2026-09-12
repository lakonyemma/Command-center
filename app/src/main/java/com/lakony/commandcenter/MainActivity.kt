package com.lakony.commandcenter

import android.Manifest
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lakony.commandcenter.absa.AbsaNfcCardReader
import com.lakony.commandcenter.notifications.TaskNotificationScheduler
import com.lakony.commandcenter.ui.CommandCenterRoot
import com.lakony.commandcenter.ui.LakonyTheme
import com.lakony.commandcenter.ui.ThemeController

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching { ThemeController.initialize(this) }
        runCatching { TaskNotificationScheduler.createChannel(this) }
        runCatching { TaskNotificationScheduler.rescheduleCached(this) }
        nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(this) }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }

        setContent {
            LakonyTheme {
                CommandCenterRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            nfcAdapter?.enableReaderMode(
                this,
                { tag -> runCatching { AbsaNfcCardReader.read(tag) } },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }
    }

    override fun onPause() {
        runCatching { nfcAdapter?.disableReaderMode(this) }
        super.onPause()
    }
}
