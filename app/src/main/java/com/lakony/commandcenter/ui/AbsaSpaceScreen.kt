package com.lakony.commandcenter.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.absa.*
import kotlinx.coroutines.launch

private val AbsaRed = Color(0xFFB0003A)
private val AbsaRedDark = Color(0xFF7E002A)
private val AbsaTint = Color(0xFF2A111A)
private val AbsaSoft = Color(0xFFFFDCE8)

@Composable
fun AbsaSpaceScreen(gateway: AbsaGateway = remember { MockAbsaGateway() }) {
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<AbsaFinanceSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            gateway.loadFinanceSnapshot()
                .onSuccess { snapshot = it }
                .onFailure { error = it.message ?: "Unable to load Absa data." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("ABSA SPACE", style = MaterialTheme.typography.headlineMedium, color = AbsaSoft)
                Text("Banking data and your contactless card", color = MutedText)
            }
            Surface(color = AbsaTint, shape = RoundedCornerShape(999.dp)) {
                Text(
                    "SANDBOX",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = AbsaSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        }

        AbsaNfcCardPanel()

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = AbsaRed)
            error != null -> {
                Card(colors = CardDefaults.cardColors(containerColor = AbsaTint)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ABSA CONNECTION", fontWeight = FontWeight.Bold, color = AbsaSoft)
                        Text(error ?: "Unknown error")
                        Button(onClick = { refresh() }, colors = ButtonDefaults.buttonColors(containerColor = AbsaRed)) { Text("TRY AGAIN") }
                    }
                }
            }
            snapshot != null -> AbsaDashboard(snapshot!!, gateway.paymentInitiationEnabled, ::refresh)
        }
    }
}

@Composable
private fun AbsaNfcCardPanel() {
    val context = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val state by AbsaNfcCardReader.state.collectAsState()

    Card(colors = CardDefaults.cardColors(containerColor = AbsaTint)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("MY DEBIT CARD", color = AbsaSoft, fontWeight = FontWeight.Bold)
                    Text("NFC contactless reader", color = MutedText, fontSize = 11.sp)
                }
                Surface(color = AbsaRedDark, shape = RoundedCornerShape(999.dp)) {
                    Text("NFC", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            when {
                adapter == null -> {
                    Text("This phone does not report NFC hardware.", fontWeight = FontWeight.SemiBold)
                }
                !adapter.isEnabled -> {
                    Text("NFC is off. Turn it on, then return here.", fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                                .onFailure { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AbsaRed),
                    ) { Text("TURN ON NFC") }
                }
                else -> when (val current = state) {
                    NfcCardState.Idle -> {
                        Text("Hold your contactless Absa debit card against the NFC area on the back of your phone.")
                        Text("The app reads permitted EMV identification data only. It does not request your PIN, CVV, or payment cryptograms.", color = MutedText, fontSize = 11.sp)
                    }
                    NfcCardState.Reading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = AbsaRed)
                        Text("Reading card. Keep it still against your phone.", fontWeight = FontWeight.SemiBold)
                    }
                    is NfcCardState.Error -> {
                        Text(current.message, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { AbsaNfcCardReader.reset() }) { Text("TRY AGAIN") }
                    }
                    is NfcCardState.Success -> {
                        val card = current.card
                        Text(card.paymentNetwork, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(card.applicationLabel, color = AbsaSoft)
                        card.maskedPan?.let { CardDetail("CARD", it) }
                        card.expiry?.let { CardDetail("EXPIRY", it) }
                        CardDetail("APPLICATION ID", card.aid.chunked(4).joinToString(" "))
                        CardDetail("LAST SCAN", card.scannedAt)
                        OutlinedButton(onClick = { AbsaNfcCardReader.reset() }) { Text("SCAN AGAIN") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AbsaDashboard(snapshot: AbsaFinanceSnapshot, paymentsEnabled: Boolean, refresh: () -> Unit) {
    val currency = snapshot.account.currency

    Card(colors = CardDefaults.cardColors(containerColor = AbsaRedDark)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AVAILABLE BALANCE", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(formatMoney(snapshot.account.availableBalance, currency), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(snapshot.account.accountName, color = Color.White)
            Text(snapshot.account.maskedAccountNumber, color = Color.White.copy(alpha = 0.78f))
            OutlinedButton(onClick = refresh, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("REFRESH") }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceMetric("UPCOMING", formatMoney(snapshot.upcomingTotal, currency), Modifier.weight(1f))
        FinanceMetric("PROJECTED", formatMoney(snapshot.projectedBalance, currency), Modifier.weight(1f))
    }

    Text("RECENT TRANSACTIONS", style = MaterialTheme.typography.labelLarge, color = AbsaSoft)
    snapshot.transactions.forEach { transaction ->
        Card {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(transaction.description, fontWeight = FontWeight.SemiBold)
                    Text("${transaction.date} • ${transaction.category.label}", color = MutedText, fontSize = 11.sp)
                }
                Text(formatSignedMoney(transaction.amount, currency), fontWeight = FontWeight.Bold)
            }
        }
    }

    Text("BUDGETS", style = MaterialTheme.typography.labelLarge, color = AbsaSoft)
    snapshot.budgets.forEach { budget ->
        val spent = snapshot.transactions.filter { it.category == budget.category && it.amount < 0 }.sumOf { -it.amount }
        val progress = if (budget.limit > 0) (spent.toFloat() / budget.limit.toFloat()).coerceIn(0f, 1f) else 0f
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(budget.category.label, fontWeight = FontWeight.SemiBold)
                    Text("${formatMoney(spent, currency)} / ${formatMoney(budget.limit, currency)}", fontSize = 11.sp)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AbsaRed)
            }
        }
    }

    Text("PLANNED EXPENSES", style = MaterialTheme.typography.labelLarge, color = AbsaSoft)
    snapshot.plannedExpenses.forEach { expense ->
        Card {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.SemiBold)
                    Text("Due ${expense.dueDate} • ${expense.category.label}", color = MutedText, fontSize = 11.sp)
                }
                Text(formatMoney(expense.amount, currency), fontWeight = FontWeight.Bold)
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = AbsaTint)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PAYMENT INITIATION", style = MaterialTheme.typography.labelLarge, color = AbsaSoft)
            Text(if (paymentsEnabled) "Approved payment API access is active." else "Locked until Absa approves payment initiation API access.", fontWeight = FontWeight.SemiBold)
            Button(
                onClick = {},
                enabled = paymentsEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AbsaRed),
            ) { Text(if (paymentsEnabled) "MAKE PAYMENT" else "PENDING ABSA APPROVAL") }
        }
    }

    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("API SCOPE", style = MaterialTheme.typography.labelLarge, color = AbsaSoft)
            Text("✓ Account balance")
            Text("✓ Recent transactions")
            Text("✓ Spending categories")
            Text("✓ Budgets and scheduled spending")
            Text("✓ Upcoming planned expenses")
            Text("○ Payment initiation, pending approval")
        }
    }
}

@Composable
private fun FinanceMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun formatMoney(amount: Long, currency: String): String = "$currency ${"%,d".format(amount)}"
private fun formatSignedMoney(amount: Long, currency: String): String {
    val prefix = if (amount >= 0) "+" else "−"
    return "$prefix$currency ${"%,d".format(kotlin.math.abs(amount))}"
}
