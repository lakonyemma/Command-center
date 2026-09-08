package com.lakony.commandcenter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.lakony.commandcenter.revenue.LeadStage
import com.lakony.commandcenter.revenue.RevenueApiClient
import com.lakony.commandcenter.revenue.RevenueAuthStore
import com.lakony.commandcenter.revenue.RevenueCustomer
import com.lakony.commandcenter.revenue.RevenueInsights
import com.lakony.commandcenter.revenue.RevenueLead
import com.lakony.commandcenter.revenue.RevenueStore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val REVENUE_GOOGLE_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
private const val REVENUE_ACCOUNT = "lakonyemmanuel92@gmail.com"

@Composable
fun RevenueDashboardScreen() {
    val context = LocalContext.current
    val activity = context.findActivity()
    if (activity == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Revenue OS")
            Text("Google authorization is unavailable in this screen context.")
        }
        return
    }

    val localStore = remember { RevenueStore(context.applicationContext) }
    val authStore = remember { RevenueAuthStore(context.applicationContext) }
    val googleClient = remember { Identity.getAuthorizationClient(activity) }
    val scope = rememberCoroutineScope()

    var workspace by remember { mutableStateOf(localStore.loadWorkspace()) }
    var token by remember { mutableStateOf(authStore.googleAccessToken()) }
    var status by remember { mutableStateOf("Restoring Revenue OS cloud connection…") }
    var busy by remember { mutableStateOf(false) }
    var cloudConnected by remember { mutableStateOf(false) }
    var showLeadDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<RevenueCustomer?>(null) }
    var invoiceCustomer by remember { mutableStateOf<RevenueCustomer?>(null) }

    fun googleError(error: Throwable): String {
        if (error is ApiException && error.statusCode == 8) {
            return "Google OAuth is not registered for this APK signing key (code 8). Reinstall the latest signed build after the Android OAuth client is registered."
        }
        return if (error is ApiException) {
            "Google authorization failed (${error.statusCode}). ${error.message.orEmpty()}".trim()
        } else {
            error.message ?: "Google authorization failed"
        }
    }

    fun syncWithToken(accessToken: String) {
        if (accessToken.isBlank()) return
        busy = true
        status = "Syncing Revenue OS cloud…"
        scope.launch {
            runCatching { RevenueApiClient.loadWorkspace(accessToken) }
                .onSuccess {
                    workspace = it
                    cloudConnected = true
                    status = "Cloud connected · Revenue OS live"
                }
                .onFailure { error ->
                    cloudConnected = false
                    val detail = error.message.orEmpty()
                    if (detail.contains("401") || detail.contains("403")) {
                        authStore.clearGoogleAccessToken()
                        token = ""
                        status = "Google session expired or was rejected. Tap Connect Google to renew it."
                    } else {
                        status = error.message ?: "Revenue cloud sync failed"
                    }
                }
            busy = false
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            status = "Google authorization was cancelled."
            busy = false
        } else {
            runCatching { googleClient.getAuthorizationResultFromIntent(data) }
                .onSuccess { authorization ->
                    val accessToken = authorization.accessToken.orEmpty()
                    if (accessToken.isBlank()) {
                        status = "Google completed authorization but returned no access token."
                        busy = false
                    } else {
                        token = accessToken
                        authStore.saveGoogleAccessToken(accessToken)
                        syncWithToken(accessToken)
                    }
                }
                .onFailure { error ->
                    status = googleError(error)
                    busy = false
                }
        }
    }

    fun authorizeGoogle(interactive: Boolean) {
        busy = true
        status = if (interactive) {
            "Choose $REVENUE_ACCOUNT and approve Gmail read-only access…"
        } else {
            "Restoring Google authorization…"
        }

        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(REVENUE_GOOGLE_SCOPE)))
        if (interactive) builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)

        googleClient.authorize(builder.build())
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    if (!interactive) {
                        status = "Google permission is ready. Tap Connect Google once to approve access."
                        busy = false
                        return@addOnSuccessListener
                    }
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent == null) {
                        status = "Google authorization is unavailable on this device."
                        busy = false
                    } else {
                        authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    }
                } else {
                    val accessToken = authorization.accessToken.orEmpty()
                    if (accessToken.isBlank()) {
                        status = "Google did not return an access token."
                        busy = false
                    } else {
                        token = accessToken
                        authStore.saveGoogleAccessToken(accessToken)
                        syncWithToken(accessToken)
                    }
                }
            }
            .addOnFailureListener { error ->
                status = googleError(error)
                busy = false
            }
    }

    fun runCloudAction(action: suspend () -> Unit) {
        if (token.isBlank() || !cloudConnected) {
            status = "Connect Revenue OS cloud before making cloud changes."
            return
        }
        busy = true
        scope.launch {
            runCatching {
                action()
                RevenueApiClient.loadWorkspace(token)
            }.onSuccess {
                workspace = it
                cloudConnected = true
                status = "Cloud connected · Revenue OS live"
            }.onFailure { error ->
                cloudConnected = false
                status = error.message ?: "Revenue cloud action failed"
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        // If Google access was already granted, AuthorizationClient returns a fresh token
        // without opening a chooser. This avoids reusing an expired cached token.
        authorizeGoogle(interactive = false)
    }

    val summary = workspace.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Revenue OS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Live business records with Google-authenticated cloud sync.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDone, contentDescription = null)
                        Text(if (cloudConnected) "CLOUD LIVE" else "CLOUD CONNECTION", fontWeight = FontWeight.Bold)
                    }
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    if (!cloudConnected) {
                        Button(
                            onClick = { authorizeGoogle(interactive = true) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Connect Google for Revenue OS") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { authorizeGoogle(interactive = false) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Refresh") }
                            OutlinedButton(
                                onClick = {
                                    authStore.clearGoogleAccessToken()
                                    token = ""
                                    cloudConnected = false
                                    status = "Cloud disconnected"
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Disconnect") }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RevenueMetricCard(Modifier.weight(1f), "MRR", ugx(summary.monthlyRecurringRevenueUgx)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                }
                RevenueMetricCard(Modifier.weight(1f), "Collected", ugx(summary.collectedRevenueUgx)) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RevenueMetricCard(Modifier.weight(1f), "Customers", summary.activeCustomers.toString()) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                }
                RevenueMetricCard(Modifier.weight(1f), "Outstanding", ugx(summary.outstandingRevenueUgx)) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(modifier = Modifier.weight(1f), enabled = !busy, onClick = { showLeadDialog = true }) { Text("Add lead") }
                Button(modifier = Modifier.weight(1f), enabled = !busy, onClick = { showCustomerDialog = true }) { Text("Add customer") }
            }
        }

        item {
            SectionCard(
                "Sales pipeline",
                listOf(
                    "Qualified leads: ${summary.qualifiedLeads}",
                    "Conversion rate: ${"%.1f".format(summary.conversionRatePercent)}%",
                    "Active subscriptions: ${summary.activeSubscriptions}",
                ),
            )
        }

        item { SectionCard("Smith priorities", RevenueInsights.recommendations(workspace)) }

        if (workspace.leads.isNotEmpty()) {
            item { Text("Leads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(workspace.leads, key = { it.id }) { lead ->
                LeadCard(lead) {
                    if (cloudConnected) {
                        runCloudAction { RevenueApiClient.updateLeadStage(token, lead.id, nextStage(lead.stage)) }
                    } else {
                        workspace = localStore.updateLeadStage(lead.id, nextStage(lead.stage))
                    }
                }
            }
        }

        if (workspace.customers.isNotEmpty()) {
            item { Text("Customers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(workspace.customers, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    onRecordPayment = { paymentCustomer = customer },
                    onCreateInvoice = { invoiceCustomer = customer },
                )
            }
        }

        if (workspace.payments.isNotEmpty()) {
            item {
                SectionCard(
                    "Recent payments",
                    workspace.payments.take(5).map { payment ->
                        val customerName = workspace.customers.firstOrNull { it.id == payment.customerId }?.name ?: "Customer"
                        "$customerName · ${ugx(payment.amountUgx)}${if (payment.reference.isBlank()) "" else " · ${payment.reference}"}"
                    },
                )
            }
        }
    }

    if (showLeadDialog) {
        AddLeadDialog(
            onDismiss = { showLeadDialog = false },
            onSave = { name, company, contact, value ->
                if (cloudConnected) runCloudAction { RevenueApiClient.addLead(token, name, company, contact, value) }
                else workspace = localStore.addLead(name, company, contact, value)
                showLeadDialog = false
            },
        )
    }

    if (showCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showCustomerDialog = false },
            onSave = { name, company, contact, plan, value ->
                if (cloudConnected) runCloudAction { RevenueApiClient.addCustomer(token, name, company, contact, plan, value) }
                else workspace = localStore.addCustomer(name, company, contact, plan, value)
                showCustomerDialog = false
            },
        )
    }

    paymentCustomer?.let { customer ->
        RecordPaymentDialog(
            customer = customer,
            onDismiss = { paymentCustomer = null },
            onSave = { amount, reference ->
                if (cloudConnected) runCloudAction { RevenueApiClient.addPayment(token, customer.id, amount, reference) }
                else workspace = localStore.addPayment(customer.id, amount, reference)
                paymentCustomer = null
            },
        )
    }

    invoiceCustomer?.let { customer ->
        CreateInvoiceDialog(
            customer = customer,
            onDismiss = { invoiceCustomer = null },
            onSave = { amount, dueDays ->
                val dueAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(dueDays.toLong())
                if (cloudConnected) runCloudAction { RevenueApiClient.addInvoice(token, customer.id, amount, dueAt) }
                else workspace = localStore.addInvoice(customer.id, amount, dueAt)
                invoiceCustomer = null
            },
        )
    }
}

@Composable
private fun LeadCard(lead: RevenueLead, onAdvance: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
private fun CustomerCard(customer: RevenueCustomer, onRecordPayment: () -> Unit, onCreateInvoice: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (customer.company.isNotBlank()) Text(customer.company)
            Text("${customer.plan} · ${ugx(customer.monthlyValueUgx)}/month")
            Text(if (customer.subscriptionActive) "Subscription active" else "Subscription inactive")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRecordPayment, modifier = Modifier.weight(1f)) { Text("Payment") }
                OutlinedButton(onClick = onCreateInvoice, modifier = Modifier.weight(1f)) { Text("Invoice") }
            }
        }
    }
}

@Composable
private fun AddLeadDialog(onDismiss: () -> Unit, onSave: (String, String, String, Long) -> Unit) {
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
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, company, contact, value.toLongOrNull() ?: 0) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddCustomerDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, Long) -> Unit) {
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
        confirmButton = { TextButton(enabled = name.isNotBlank() && (value.toLongOrNull() ?: 0) > 0, onClick = { onSave(name, company, contact, plan, value.toLongOrNull() ?: 0) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RecordPaymentDialog(customer: RevenueCustomer, onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
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
        confirmButton = { TextButton(enabled = (amount.toLongOrNull() ?: 0) > 0, onClick = { onSave(amount.toLongOrNull() ?: 0, reference) }) { Text("Record") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CreateInvoiceDialog(customer: RevenueCustomer, onDismiss: () -> Unit, onSave: (Long, Int) -> Unit) {
    var amount by remember { mutableStateOf(customer.monthlyValueUgx.toString()) }
    var dueDays by remember { mutableStateOf("7") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(customer.name)
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text("Amount UGX") })
                OutlinedTextField(value = dueDays, onValueChange = { dueDays = it.filter(Char::isDigit) }, label = { Text("Due in days") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = (amount.toLongOrNull() ?: 0) > 0 && (dueDays.toIntOrNull() ?: 0) > 0,
                onClick = { onSave(amount.toLongOrNull() ?: 0, dueDays.toIntOrNull() ?: 7) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RevenueMetricCard(modifier: Modifier = Modifier, title: String, value: String, icon: @Composable () -> Unit) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            icon()
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
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

private fun ugx(value: Long): String = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(value)}"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
