package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.absa.*
import kotlinx.coroutines.launch

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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("ABSA SPACE", style = MaterialTheme.typography.headlineMedium)
                Text("Banking data inside your Command Center", color = MutedText)
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    "SANDBOX",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        }

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ABSA CONNECTION", fontWeight = FontWeight.Bold)
                        Text(error ?: "Unknown error")
                        Button(onClick = { refresh() }) { Text("TRY AGAIN") }
                    }
                }
            }
            snapshot != null -> AbsaDashboard(snapshot!!, gateway.paymentInitiationEnabled, ::refresh)
        }
    }
}

@Composable
private fun AbsaDashboard(
    snapshot: AbsaFinanceSnapshot,
    paymentsEnabled: Boolean,
    refresh: () -> Unit,
) {
    val currency = snapshot.account.currency

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "AVAILABLE BALANCE",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                formatMoney(snapshot.account.availableBalance, currency),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(snapshot.account.accountName, color = MaterialTheme.colorScheme.onPrimary)
            Text(snapshot.account.maskedAccountNumber, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f))
            OutlinedButton(
                onClick = refresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text("REFRESH") }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceMetric("UPCOMING", formatMoney(snapshot.upcomingTotal, currency), Modifier.weight(1f))
        FinanceMetric("PROJECTED", formatMoney(snapshot.projectedBalance, currency), Modifier.weight(1f))
    }

    Text("RECENT TRANSACTIONS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
    snapshot.transactions.forEach { transaction ->
        Card {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(transaction.description, fontWeight = FontWeight.SemiBold)
                    Text("${transaction.date} • ${transaction.category.label}", color = MutedText, fontSize = 11.sp)
                }
                Text(formatSignedMoney(transaction.amount, currency), fontWeight = FontWeight.Bold)
            }
        }
    }

    Text("BUDGETS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
    snapshot.budgets.forEach { budget ->
        val spent = snapshot.transactions
            .filter { it.category == budget.category && it.amount < 0 }
            .sumOf { -it.amount }
        val progress = if (budget.limit > 0) (spent.toFloat() / budget.limit.toFloat()).coerceIn(0f, 1f) else 0f
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(budget.category.label, fontWeight = FontWeight.SemiBold)
                    Text("${formatMoney(spent, currency)} / ${formatMoney(budget.limit, currency)}", fontSize = 11.sp)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    Text("PLANNED EXPENSES", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
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

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PAYMENT INITIATION", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
            Text(
                if (paymentsEnabled) "Approved payment API access is active."
                else "Locked until Absa approves payment initiation API access.",
                fontWeight = FontWeight.SemiBold,
            )
            Button(onClick = {}, enabled = paymentsEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (paymentsEnabled) "MAKE PAYMENT" else "PENDING ABSA APPROVAL")
            }
        }
    }

    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("API SCOPE", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
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

private fun formatMoney(amount: Long, currency: String): String =
    "$currency ${"%,d".format(amount)}"

private fun formatSignedMoney(amount: Long, currency: String): String {
    val prefix = if (amount >= 0) "+" else "−"
    return "$prefix$currency ${"%,d".format(kotlin.math.abs(amount))}"
}
