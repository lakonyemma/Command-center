package com.lakony.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalendarHubScreen() {
    HubScreen(
        title = "CALENDAR",
        subtitle = "Your schedule, tasks and reminders in one place.",
        items = listOf(
            HubItem(Icons.Default.CalendarMonth, "Google Calendar", "Connection-ready calendar integration", "READY"),
            HubItem(Icons.Default.NotificationsActive, "Task reminders", "Sound and vibration notification pipeline", "ACTIVE"),
            HubItem(Icons.Default.Sync, "Taskly due dates", "Task sync surface for Taskly deadlines", "READY"),
        ),
    )
}

@Composable
fun InboxHubScreen() {
    HubScreen(
        title = "INBOX",
        subtitle = "Mail and communication control surface.",
        items = listOf(
            HubItem(Icons.Default.Email, "Gmail", "Read, search, summarize and prepare replies", "CONNECT"),
            HubItem(Icons.Default.AlternateEmail, "Priority inbox", "Important messages and action items", "READY"),
            HubItem(Icons.Default.NotificationsActive, "Mail alerts", "Notify when selected messages need attention", "READY"),
        ),
    )
}

@Composable
fun DeveloperHubScreen() {
    HubScreen(
        title = "DEVELOPER",
        subtitle = "Projects, source control and deployment status.",
        items = listOf(
            HubItem(Icons.Default.Code, "GitHub", "Repositories, issues, pull requests and commits", "CONNECTED"),
            HubItem(Icons.Default.Cloud, "Cloud", "Render services, deploys, logs and metrics", "READY"),
            HubItem(Icons.Default.Storage, "Databases", "PostgreSQL service status and data tools", "READY"),
            HubItem(Icons.Default.Security, "Security", "Secrets stay outside the repository", "ENFORCED"),
        ),
    )
}

@Composable
fun TradingHubScreen() {
    var riskGuard by remember { mutableStateOf(true) }
    var newsGuard by remember { mutableStateOf(true) }
    var tradeAlerts by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("TRADING", style = MaterialTheme.typography.headlineMedium)
        Text("MT5 tools, risk controls, journals and market alerts.", color = MutedText)

        TradingToggle("Risk guard", "Account protection and position sizing", riskGuard) { riskGuard = it }
        TradingToggle("News guard", "Block or flag entries around major events", newsGuard) { newsGuard = it }
        TradingToggle("Trade alerts", "Surface trade and risk notifications", tradeAlerts) { tradeAlerts = it }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TOOLS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                ToolLine(Icons.Default.AutoGraph, "EA control panel", "Configuration surface for your MT5 scalping EA")
                ToolLine(Icons.Default.Timeline, "Trading journal", "Track entries, exits, risk and results")
                ToolLine(Icons.Default.Bolt, "Market alerts", "Gold, FX and crypto watch surfaces")
            }
        }
    }
}

@Composable
fun DevicesHubScreen() {
    HubScreen(
        title = "DEVICES",
        subtitle = "Phone, computer, IoT and automation controls.",
        items = listOf(
            HubItem(Icons.Default.Devices, "Android device", "Notification and app automation surface", "LOCAL"),
            HubItem(Icons.Default.Memory, "IoT", "ESP32, Raspberry Pi and sensor integration space", "READY"),
            HubItem(Icons.Default.Cloud, "Remote automation", "Cloud-triggered workflows and device commands", "READY"),
        ),
    )
}

@Composable
fun SystemsHubScreen() {
    HubScreen(
        title = "SYSTEMS",
        subtitle = "Infrastructure, automation and service health.",
        items = listOf(
            HubItem(Icons.Default.Cloud, "Render", "Services, deploys and logs", "READY"),
            HubItem(Icons.Default.Storage, "PostgreSQL", "Database health and data services", "READY"),
            HubItem(Icons.Default.Sync, "Automations", "Scheduled jobs, reminders and condition checks", "READY"),
            HubItem(Icons.Default.AccountBalance, "Finance connections", "Absa and future payment providers", "PARTIAL"),
        ),
    )
}

private data class HubItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val status: String,
)

@Composable
private fun HubScreen(title: String, subtitle: String, items: List<HubItem>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MutedText)
        items.forEach { item ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.description, color = MutedText, fontSize = 12.sp)
                    }
                    AssistChip(onClick = {}, label = { Text(item.status, fontSize = 9.sp) })
                }
            }
        }
    }
}

@Composable
private fun TradingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedText, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ToolLine(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(2.dp))
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
}
