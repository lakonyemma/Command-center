package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakony.commandcenter.revenue.RevenueWorkspace
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RevenueDashboardScreen() {
    val workspace = remember { RevenueWorkspace() }
    val summary = workspace.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Revenue OS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Real revenue, customers, leads and subscriptions in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RevenueMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "MRR",
                    value = ugx(summary.monthlyRecurringRevenueUgx),
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                )
                RevenueMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Collected",
                    value = ugx(summary.collectedRevenueUgx),
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RevenueMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Customers",
                    value = summary.activeCustomers.toString(),
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                )
                RevenueMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Outstanding",
                    value = ugx(summary.outstandingRevenueUgx),
                    icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                )
            }
        }

        item {
            SectionCard(
                title = "Sales pipeline",
                lines = listOf(
                    "Qualified leads: ${summary.qualifiedLeads}",
                    "Conversion rate: ${"%.1f".format(summary.conversionRatePercent)}%",
                    "Active subscriptions: ${summary.activeSubscriptions}",
                ),
            )
        }

        item {
            SectionCard(
                title = "Revenue actions",
                lines = listOf(
                    "Add and qualify leads",
                    "Convert won leads into customers",
                    "Track subscription value and unpaid revenue",
                    "Connect payment records before reporting income",
                ),
            )
        }

        item {
            SectionCard(
                title = "Smith business commands",
                lines = listOf(
                    "Show today's revenue status",
                    "Find leads that need follow-up",
                    "Show unpaid customer balances",
                    "Rank customers by monthly value",
                    "Prepare a sales follow-up plan",
                ),
            )
        }
    }
}

@Composable
private fun RevenueMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun ugx(value: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(value)}"
}
