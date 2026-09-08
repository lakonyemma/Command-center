package com.lakony.commandcenter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmithRevenueScreen() {
    val context = LocalContext.current
    var autoPost by remember { mutableStateOf(true) }
    var autoScout by remember { mutableStateOf(true) }
    var approvalGate by remember { mutableStateOf(true) }
    var creatorStudioOpen by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

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
            subtitle = "TikTok, Pinterest and YouTube publishing, captions, schedules and performance feedback. Tap to open.",
            icon = Icons.Default.VideoLibrary,
            status = if (creatorStudioOpen) "OPEN" else "READY",
            onClick = { creatorStudioOpen = !creatorStudioOpen },
            expanded = creatorStudioOpen,
        )

        if (creatorStudioOpen) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("CREATOR STUDIO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Manage Smith's social publishing through Metricool. Automatic organic publishing can run without approval; paid boosts still require a budget decision.",
                        color = MutedText,
                        fontSize = 12.sp,
                    )

                    Button(
                        onClick = { openUrl("https://app.metricool.com/planner") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Text("  Open Metricool Planner")
                    }

                    OutlinedButton(
                        onClick = { openUrl("https://www.tiktok.com/") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open TikTok")
                    }
                    OutlinedButton(
                        onClick = { openUrl("https://www.pinterest.com/") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open Pinterest")
                    }
                    OutlinedButton(
                        onClick = { openUrl("https://studio.youtube.com/") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open YouTube Studio")
                    }

                    Text(
                        "Tip: use Metricool Planner for the live queue, publishing status and scheduled posts. Smith's automation can keep creating and scheduling campaigns in the background.",
                        color = MutedText,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Text("AUTOMATION", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        SettingRow(
            title = "Daily product scouting",
            subtitle = "Continuously rank high-demand products and flag winners.",
            checked = autoScout,
            onCheckedChange = { autoScout = it },
        )
        SettingRow(
            title = "Automatic social publishing",
            subtitle = "Prepare and auto-publish organic TikTok, Pinterest and YouTube campaigns.",
            checked = autoPost,
            onCheckedChange = { autoPost = it },
        )
        SettingRow(
            title = "Approval for high-impact actions",
            subtitle = "Keep refunds, cancellations, destructive edits, major price changes and paid ad spend behind approval.",
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
                Text("Shopify: managed through Smith cloud", fontWeight = FontWeight.SemiBold)
                Text("Creator publishing: open Metricool Planner to view connected social accounts and queue status", color = MutedText, fontSize = 12.sp)
                Text("Supplier: Syncee is the preferred fulfillment source when available", color = MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AgentStatusCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: String,
    onClick: (() -> Unit)? = null,
    expanded: Boolean = false,
) {
    Card(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable { onClick() } else Modifier.fillMaxWidth(),
    ) {
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
            Column(horizontalAlignment = Alignment.End) {
                Text(status, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (onClick != null) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = PrimaryBlue,
                    )
                }
            }
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
