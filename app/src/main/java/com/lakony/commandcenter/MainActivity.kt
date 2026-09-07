package com.lakony.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lakony.commandcenter.ui.CommandCenterApp
import com.lakony.commandcenter.ui.LakonyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LakonyTheme {
                CommandCenterApp()
            }
        }
    }
}
