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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmithRevenueScreen() {
    var autoPost by remember { mutableStateOf(true) }
    var autoScout by remember { mutableStateOf(true) }
    var approvalGate by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SMITH REVENUE AGENT", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Creator Studio + Shopify product scout. Smith prepares daily content, finds demand, and turns winning products into campaigns.",
            color = MutedText,
        )

        AgentStatusCard(
            title = "SHOPIFY PRODUCT SCOUT",
            subtitle = "Demand discovery, product scoring, listing preparation, pricing and campaign optimization.",
            icon = Icons.Default.Storefront,
            status = "ACTIVE",
        )
        AgentStatusCard(
            title = "CREATOR STUDIO",
            subtitle = "YouTube Shorts and TikTok content queue, captions, hooks, schedules and performance feedback.",
            icon = Icons.Default.VideoLibrary,
            status = "READY",
        )

        Text("AUTOMATION", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        SettingRow(
            title = "Daily product scouting",
            subtitle = "Continuously rank high-demand products and flag winners.",
            checked = autoScout,
            onCheckedChange = { autoScout = it },
        )
        SettingRow(
            title = "Daily social publishing",
            subtitle = "Prepare and schedule daily TikTok and YouTube campaigns.",
            checked = autoPost,
            onCheckedChange = { autoPost = it },
        )
        SettingRow(
            title = "Approval for high-impact actions",
            subtitle = "Keep refunds, cancellations, destructive edits and major price changes behind approval.",
            checked = approvalGate,
            onCheckedChange = { approvalGate = it },
        )

        Text("PRODUCT SCORING", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        ScoreCard("DEMAND", "Search momentum + marketplace sales + social interest", Icons.Default.TrendingUp)
        ScoreCard("MARGIN", "Supplier cost + shipping + target selling price", Icons.Default.AutoAwesome)
        ScoreCard("FULFILLMENT", "Supplier reliability + delivery window + stock", Icons.Default.Inventory2)
        ScoreCard("WINNER TEST", "Traffic, conversion, orders and repeat demand", Icons.Default.CheckCircle)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("LIVE CONNECTIONS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text("Shopify: connected through Smith cloud", fontWeight = FontWeight.SemiBold)
                Text("Supplier: connect Zendrop/Syncee for autonomous fulfillment", color = MutedText, fontSize = 12.sp)
                Text("TikTok + YouTube: OAuth publishing setup required for direct posting", color = MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AgentStatusCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, status: String) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 12.sp)
            }
            Text(status, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedText, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ScoreCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue)
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 12.sp)
            }
        }
    }
}
