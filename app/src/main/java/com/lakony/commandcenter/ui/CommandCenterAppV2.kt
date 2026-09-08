package com.lakony.commandcenter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.data.LocalStore
import com.lakony.commandcenter.logic.SmithCommandEngine
import com.lakony.commandcenter.profile.ProfileImageStore
import com.lakony.commandcenter.taskly.TasklyApi
import com.lakony.commandcenter.taskly.TasklySessionStore
import java.util.Locale
import kotlinx.coroutines.launch

private enum class AppDestination(val label: String) {
    Home("Home"), Taskly("Taskly"), Smith("Smith"), Revenue("Revenue"), Money("Money"), Absa("Absa"), Settings("Settings")
}

@Composable
fun CommandCenterAppV2() {
    val context = LocalContext.current
    val localStore = remember { LocalStore(context) }
    val tasklyStore = remember { TasklySessionStore(context) }
    val tasklyApi = remember { TasklyApi(tasklyStore) }
    var destination by remember { mutableStateOf(AppDestination.Home) }
    var displayName by remember { mutableStateOf(localStore.loadName()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ProfessionalTopBar(displayName, destination.label) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppDestination.entries.forEach { item ->
                    val icon = when (item) {
                        AppDestination.Home -> Icons.Default.Home
                        AppDestination.Taskly -> Icons.Default.CloudSync
                        AppDestination.Smith -> Icons.Default.SmartToy
                        AppDestination.Revenue -> Icons.Default.Storefront
                        AppDestination.Money -> Icons.Default.AccountBalanceWallet
                        AppDestination.Absa -> Icons.Default.AccountBalance
                        AppDestination.Settings -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 8.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                AppDestination.Home -> ProfessionalHome(
                    name = displayName,
                    tasklyConnected = tasklyStore.isSignedIn,
                    onTaskly = { destination = AppDestination.Taskly },
                    onMoney = { destination = AppDestination.Money },
                    onAbsa = { destination = AppDestination.Absa },
                    onSmith = { destination = AppDestination.Smith },
                    onRevenue = { destination = AppDestination.Revenue },
                )
                AppDestination.Taskly -> TasklyHubScreen(tasklyStore, tasklyApi)
                AppDestination.Smith -> SmithV2(tasklyStore, tasklyApi) { target ->
                    destination = when (target.lowercase(Locale.ROOT)) {
                        "taskly", "tasks" -> AppDestination.Taskly
                        "money" -> AppDestination.Money
                        "absa", "bank", "banking" -> AppDestination.Absa
                        "revenue", "shopify", "creator", "creator studio", "tiktok", "youtube" -> AppDestination.Revenue
                        "settings" -> AppDestination.Settings
                        else -> AppDestination.Home
                    }
                }
                AppDestination.Revenue -> SmithRevenueScreen()
                AppDestination.Money -> EnhancedMoneyScreen()
                AppDestination.Absa -> AbsaSpaceScreen()
                AppDestination.Settings -> EnhancedSettingsScreen(
                    displayName = displayName,
                    onSaveName = {
                        displayName = it
                        localStore.saveName(it)
                    },
                    tasklyConnected = tasklyStore.isSignedIn,
                    tasklyUser = tasklyStore.userName,
                    onOpenTaskly = { destination = AppDestination.Taskly },
                )
            }
        }
    }
}

@Composable
private fun ProfessionalTopBar(name: String, section: String) {
    val context = LocalContext.current
    val profile = remember { ProfileImageStore(context) }
    val bitmap = remember(section, name) { profile.load() }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column {
                    Text(
                        "LAKONY / ${section.uppercase(Locale.ROOT)}",
                        color = MutedText,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "COMMAND CENTER",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }
            if (bitmap != null) {
                Image(bitmap, "Profile picture", Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.take(1).uppercase(Locale.ROOT),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalHome(
    name: String,
    tasklyConnected: Boolean,
    onTaskly: () -> Unit,
    onMoney: () -> Unit,
    onAbsa: () -> Unit,
    onSmith: () -> Unit,
    onRevenue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Hello, $name", style = MaterialTheme.typography.headlineLarge)
        Text("Everything important, in one place.", color = MutedText)

        MetricBlock("TASKLY", if (tasklyConnected) "CONNECTED" else "OFFLINE", Modifier.fillMaxWidth())

        FeatureCard(
            eyebrow = "TASKLY",
            title = if (tasklyConnected) "Tasks are connected" else "Connect your workspace",
            subtitle = "Tasks, workspaces and due reminders.",
            onClick = onTaskly,
        )

        FeatureCard(
            eyebrow = "BANKING",
            title = "Absa finance space",
            subtitle = "Balance, transactions, budgets and planned expenses.",
            onClick = onAbsa,
        )

        FeatureCard(
            eyebrow = "SMITH REVENUE AGENT",
            title = "Shopify + Creator Studio",
            subtitle = "Product scouting, daily TikTok/YouTube campaigns and sales optimization.",
            onClick = onRevenue,
        )

        Text("MODULES", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleBlock("MONEY", "Plans", Modifier.weight(1f), onMoney)
            ModuleBlock("ABSA", "Banking", Modifier.weight(1f), onAbsa)
            ModuleBlock("SMITH", "Commands", Modifier.weight(1f), onSmith)
        }
    }
}

@Composable
private fun FeatureCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = MutedText, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedText)
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MutedText)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = PrimaryBlue)
        }
    }
}

@Composable
private fun ModuleBlock(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SmithV2(
    tasklyStore: TasklySessionStore,
    api: TasklyApi,
    navigate: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Ready.") }
    var busy by remember { mutableStateOf(false) }

    fun addToTaskly(title: String) {
        val workspace = tasklyStore.selectedWorkspaceId
        if (!tasklyStore.isSignedIn) {
            response = "Connect Taskly first. Local tasks have been removed."
            navigate("taskly")
            return
        }
        if (workspace == null) {
            response = "Select a Taskly workspace first."
            navigate("taskly")
            return
        }
        busy = true
        scope.launch {
            api.createTask(workspace, title)
                .onSuccess { response = "Added to Taskly: ${it.title}" }
                .onFailure { response = it.message ?: "Taskly could not add the task." }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SMITH", style = MaterialTheme.typography.headlineMedium)
        Text(if (tasklyStore.isSignedIn) "Taskly commands active." else "Connect Taskly to create and manage tasks.", color = MutedText)
        OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), label = { Text("Command") }, minLines = 2)
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val raw = command.trim()
                val normalized = raw.lowercase(Locale.ROOT).removePrefix("smith,").trim()
                when {
                    normalized == "show my tasks" || normalized == "sync tasks" -> navigate("taskly")
                    normalized == "open absa" || normalized == "show my bank" || normalized == "show my balance" -> navigate("absa")
                    normalized == "open revenue" || normalized == "open shopify" || normalized == "open creator studio" -> navigate("revenue")
                    normalized.startsWith("add task ") -> {
                        val newTitle = raw.substringAfter("add task ", "", ignoreCase = true).trim()
                        if (newTitle.isBlank()) response = "Give the task a title." else addToTaskly(newTitle)
                    }
                    else -> {
                        val result = SmithCommandEngine.run(raw)
                        response = result.message
                        result.taskToAdd?.let { addToTaskly(it) }
                        result.destination?.let(navigate)
                    }
                }
            },
        ) { Text(if (busy) "WORKING…" else "RUN COMMAND") }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("RESPONSE", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(response)
            }
        }
    }
}
