package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class ControlModule {
    Menu, Calendar, Inbox, Developer, Trading, Devices, Systems
}

@Composable
fun ControlCenterScreen() {
    var module by remember { mutableStateOf(ControlModule.Menu) }

    when (module) {
        ControlModule.Menu -> ControlMenu { module = it }
        ControlModule.Calendar -> CalendarHubScreen()
        ControlModule.Inbox -> InboxHubScreen()
        ControlModule.Developer -> DeveloperHubScreen()
        ControlModule.Trading -> TradingHubScreen()
        ControlModule.Devices -> DevicesHubScreen()
        ControlModule.Systems -> SystemsHubScreen()
    }
}

@Composable
private fun ControlMenu(open: (ControlModule) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("CONTROL CENTER", style = MaterialTheme.typography.headlineMedium)
        Text("One place for your work, communications, trading, devices and infrastructure.", color = MutedText)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlTile(Icons.Default.CalendarMonth, "CALENDAR", "Schedule and reminders", Modifier.weight(1f)) { open(ControlModule.Calendar) }
            ControlTile(Icons.Default.Email, "INBOX", "Gmail and action items", Modifier.weight(1f)) { open(ControlModule.Inbox) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlTile(Icons.Default.Code, "DEVELOPER", "GitHub and deployments", Modifier.weight(1f)) { open(ControlModule.Developer) }
            ControlTile(Icons.Default.ShowChart, "TRADING", "MT5 tools and risk", Modifier.weight(1f)) { open(ControlModule.Trading) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlTile(Icons.Default.Devices, "DEVICES", "Phone, IoT and automation", Modifier.weight(1f)) { open(ControlModule.Devices) }
            ControlTile(Icons.Default.Hub, "SYSTEMS", "Cloud and databases", Modifier.weight(1f)) { open(ControlModule.Systems) }
        }
    }
}

@Composable
private fun ControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue)
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
