package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakony.commandcenter.revenue.RevenueStore
import com.lakony.commandcenter.smith.OpportunityStatus
import com.lakony.commandcenter.smith.SmithRevenueEngine
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SmithMissionControlScreen() {
    val context = LocalContext.current
    val engine = remember { SmithRevenueEngine(context) }
    val revenue = remember { RevenueStore(context).loadWorkspace() }
    val opportunities = remember { engine.opportunities() }
    val brief = remember { engine.morningBrief() }
    val activeOpportunities = opportunities.filter { it.status !in setOf(OpportunityStatus.WON, OpportunityStatus.DISMISSED) }
    val approvals = opportunities.filter { it.status == OpportunityStatus.REVIEWED && it.proposalDraft.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Column {
                    Text("SMITH MISSION CONTROL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Autonomous revenue operations with approval controls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TODAY'S BRIEF", style = MaterialTheme.typography.labelLarge)
                    Text(brief.headline, fontWeight = FontWeight.Bold)
                    brief.priorities.forEach { Text("• $it") }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmithMetricCard(Modifier.weight(1f), "Pipeline", activeOpportunities.size.toString(), Icons.Default.Search)
                SmithMetricCard(Modifier.weight(1f), "Approvals", approvals.size.toString(), Icons.Default.CheckCircle)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmithMetricCard(Modifier.weight(1f), "Qualified leads", revenue.summary.qualifiedLeads.toString(), Icons.Default.Campaign)
                SmithMetricCard(Modifier.weight(1f), "MRR", ugx(revenue.summary.monthlyRecurringRevenueUgx), Icons.Default.AutoAwesome)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SMITH AUTONOMY", style = MaterialTheme.typography.labelLarge)
                    Text("Smith can discover, score, research and prepare opportunities automatically.")
                    Text("External outreach, commitments, spending and irreversible actions stay behind your approval.")
                }
            }
        }

        item {
            Text("TOP OPPORTUNITIES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (activeOpportunities.isEmpty()) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No opportunities loaded yet", fontWeight = FontWeight.Bold)
                        Text("The scanner layer will feed public business opportunities into this queue. Smith will rank them before asking you to approve outreach.")
                    }
                }
            }
        } else {
            activeOpportunities.take(8).forEach { opportunity ->
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(opportunity.title, fontWeight = FontWeight.Bold)
                            Text(opportunity.company)
                            Text("Score ${opportunity.score}/100 • ${ugx(opportunity.estimatedValueUgx)}")
                            Text(opportunity.need, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Status: ${opportunity.status.name.replace('_', ' ')}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmithMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

private fun ugx(value: Long): String = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(value)}"
