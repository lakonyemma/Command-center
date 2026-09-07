package com.lakony.commandcenter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.money.MoneyStore
import com.lakony.commandcenter.money.PlannedSpend
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EnhancedMoneyScreen() {
    val context = LocalContext.current
    val store = remember { MoneyStore(context) }
    var balanceText by remember { mutableStateOf(if (store.manualBalance == 0.0) "" else store.manualBalance.toString()) }
    var spends by remember { mutableStateOf(store.loadPlannedSpends()) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf("1") }

    fun save(items: List<PlannedSpend>) {
        spends = items
        store.savePlannedSpends(items)
    }

    val balance = balanceText.toDoubleOrNull() ?: store.manualBalance
    val outstanding = spends.filterNot { it.completed }.sumOf { it.amount }
    val availableAfterPlans = balance - outstanding
    val money = NumberFormat.getCurrencyInstance(Locale("en", "UG")).apply { currency = java.util.Currency.getInstance("UGX") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MONEY", style = MaterialTheme.typography.headlineMedium)
        Text("Balance view, planned spending and Absa-ready connection.", color = MutedText)

        Card(colors = CardDefaults.cardColors(containerColor = SoftBlue)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AVAILABLE AFTER PLANS", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(money.format(availableAfterPlans), style = MaterialTheme.typography.headlineLarge)
                Text("Current balance ${money.format(balance)}  •  Planned ${money.format(outstanding)}", color = MutedText, fontSize = 12.sp)
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ABSA", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text("Live bank connection", fontWeight = FontWeight.Bold)
                Text("Prepared for an Absa-authorized open-banking connection. Command Center never stores your Absa password, PIN, OTP or card credentials.", color = MutedText, fontSize = 12.sp)
                Text("Status: awaiting bank-authorized API consent", color = PrimaryBlue, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.absa.co.ug/open-banking/")))
                    }) { Text("Open Absa consent info") }
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.absa.co.ug/absa-app/")))
                    }) { Text("Absa app") }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("BALANCE", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current balance (UGX)") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        store.manualBalance = balanceText.toDoubleOrNull() ?: 0.0
                        balanceText = store.manualBalance.toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save balance") }
                Text("Manual until Absa grants an authorized account-data connection.", color = MutedText, fontSize = 11.sp)
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SCHEDULE SPENDING", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("What are you paying for?") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount (UGX)") }, singleLine = true)
                OutlinedTextField(value = daysFromNow, onValueChange = { daysFromNow = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Days from now") }, singleLine = true)
                Button(
                    onClick = {
                        val value = amount.toDoubleOrNull() ?: return@Button
                        val days = daysFromNow.toLongOrNull() ?: 0L
                        if (title.isBlank() || value <= 0) return@Button
                        save(
                            spends + PlannedSpend(
                                title = title.trim(),
                                amount = value,
                                dueAtMillis = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L,
                            )
                        )
                        title = ""
                        amount = ""
                        daysFromNow = "1"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add planned spending") }
                Text("This schedules the expense in Command Center. It does not move money from your bank account.", color = MutedText, fontSize = 11.sp)
            }
        }

        Text("PLANNED", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        if (spends.isEmpty()) Text("No scheduled spending yet.", color = MutedText)
        spends.sortedBy { it.dueAtMillis }.forEach { item ->
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.completed,
                        onCheckedChange = { checked -> save(spends.map { if (it.id == item.id) it.copy(completed = checked) else it }) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text("${money.format(item.amount)} • ${dateFormat.format(Date(item.dueAtMillis))}", color = MutedText, fontSize = 11.sp)
                    }
                    TextButton(onClick = { save(spends.filterNot { it.id == item.id }) }) { Text("Remove") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
