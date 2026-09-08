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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakony.commandcenter.revenue.LeadStage
import com.lakony.commandcenter.revenue.RevenueCustomer
import com.lakony.commandcenter.revenue.RevenueLead
import com.lakony.commandcenter.revenue.RevenueStore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RevenueDashboardScreen() {
    val context = LocalContext.current
    val store = remember { RevenueStore(context.applicationContext) }
    var workspace by remember { mutableStateOf(store.loadWorkspace()) }
    var showLeadDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<RevenueCustomer?>(null) }

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
                text = "Live business records. No demo revenue.",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { showLeadDialog = true },
                ) {
                    Text("Add lead")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { showCustomerDialog = true },
                ) {
                    Text("Add customer")
                }
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

        if (workspace.leads.isNotEmpty()) {
            item {
                Text("Leads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(workspace.leads.size) { index ->
                val lead = workspace.leads[index]
                LeadCard(
                    lead = lead,
                    onAdvance = {
                        workspace = store.updateLeadStage(lead.id, nextStage(lead.stage))
                    },
                )
            }
        }

        if (workspace.customers.isNotEmpty()) {
            item {
                Text("Customers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(workspace.customers.size) { index ->
                val customer = workspace.customers[index]
                CustomerCard(
                    customer = customer,
                    onRecordPayment = { paymentCustomer = customer },
                )
            }
        }

        if (workspace.payments.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Recent payments",
                    lines = workspace.payments.take(5).map { payment ->
                        val customerName = workspace.customers.firstOrNull { it.id == payment.customerId }?.name ?: "Customer"
                        "$customerName · ${ugx(payment.amountUgx)}${if (payment.reference.isBlank()) "" else " · ${payment.reference}"}"
                    },
                )
            }
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

    if (showLeadDialog) {
        AddLeadDialog(
            onDismiss = { showLeadDialog = false },
            onSave = { name, company, contact, value ->
                workspace = store.addLead(name, company, contact, value)
                showLeadDialog = false
            },
        )
    }

    if (showCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showCustomerDialog = false },
            onSave = { name, company, contact, plan, value ->
                workspace = store.addCustomer(name, company, contact, plan, value)
                showCustomerDialog = false
            },
        )
    }

    paymentCustomer?.let { customer ->
        RecordPaymentDialog(
            customer = customer,
            onDismiss = { paymentCustomer = null },
            onSave = { amount, reference ->
                workspace = store.addPayment(customer.id, amount, reference)
                paymentCustomer = null
            },
        )
    }
}

@Composable
private fun LeadCard(lead: RevenueLead, onAdvance: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(lead.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (lead.company.isNotBlank()) Text(lead.company)
            if (lead.contact.isNotBlank()) Text(lead.contact, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Stage: ${lead.stage.name.replace('_', ' ')}")
            Text("Potential: ${ugx(lead.estimatedMonthlyValueUgx)}/month")
            if (lead.stage != LeadStage.WON && lead.stage != LeadStage.LOST) {
                TextButton(onClick = onAdvance) { Text("Advance stage") }
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: RevenueCustomer, onRecordPayment: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (customer.company.isNotBlank()) Text(customer.company)
            Text("${customer.plan} · ${ugx(customer.monthlyValueUgx)}/month")
            Text(if (customer.subscriptionActive) "Subscription active" else "Subscription inactive")
            Button(onClick = onRecordPayment) { Text("Record payment") }
        }
    }
}

@Composable
private fun AddLeadDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add lead") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") })
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Phone or email") })
                OutlinedTextField(value = value, onValueChange = { value = it.filter(Char::isDigit) }, label = { Text("Potential monthly UGX") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, company, contact, value.toLongOrNull() ?: 0) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("Business") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add paying customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") })
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Phone or email") })
                OutlinedTextField(value = plan, onValueChange = { plan = it }, label = { Text("Plan") })
                OutlinedTextField(value = value, onValueChange = { value = it.filter(Char::isDigit) }, label = { Text("Monthly UGX") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (value.toLongOrNull() ?: 0) > 0,
                onClick = { onSave(name, company, contact, plan, value.toLongOrNull() ?: 0) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RecordPaymentDialog(
    customer: RevenueCustomer,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit,
) {
    var amount by remember { mutableStateOf(customer.monthlyValueUgx.toString()) }
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(customer.name)
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text("Amount UGX") })
                OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("Payment reference") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = (amount.toLongOrNull() ?: 0) > 0,
                onClick = { onSave(amount.toLongOrNull() ?: 0, reference) },
            ) { Text("Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
            lines.forEach { line -> Text(line, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private fun nextStage(stage: LeadStage): LeadStage = when (stage) {
    LeadStage.NEW -> LeadStage.CONTACTED
    LeadStage.CONTACTED -> LeadStage.QUALIFIED
    LeadStage.QUALIFIED -> LeadStage.PROPOSAL
    LeadStage.PROPOSAL -> LeadStage.WON
    LeadStage.WON -> LeadStage.WON
    LeadStage.LOST -> LeadStage.LOST
}

private fun ugx(value: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(value)}"
}
