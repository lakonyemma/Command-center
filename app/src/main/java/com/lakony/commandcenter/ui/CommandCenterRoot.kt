package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class RootDestination {
    Dashboard, Smith, Revenue, Control
}

@Composable
fun CommandCenterRoot() {
    var root by remember { mutableStateOf(RootDestination.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = root == RootDestination.Dashboard,
                    onClick = { root = RootDestination.Dashboard },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = root == RootDestination.Smith,
                    onClick = { root = RootDestination.Smith },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Smith") },
                    label = { Text("Smith") },
                )
                NavigationBarItem(
                    selected = root == RootDestination.Revenue,
                    onClick = { root = RootDestination.Revenue },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Revenue") },
                    label = { Text("Revenue") },
                )
                NavigationBarItem(
                    selected = root == RootDestination.Control,
                    onClick = { root = RootDestination.Control },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Control Center") },
                    label = { Text("Control") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (root) {
                RootDestination.Dashboard -> CommandCenterAppV2()
                RootDestination.Smith -> SmithMissionControlScreen()
                RootDestination.Revenue -> RevenueDashboardScreen()
                RootDestination.Control -> ControlCenterScreen()
            }
        }
    }
}
