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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.data.LocalStore
import com.lakony.commandcenter.logic.SmithCommandEngine
import com.lakony.commandcenter.model.AppTask
import com.lakony.commandcenter.profile.ProfileImageStore
import com.lakony.commandcenter.taskly.TasklyApi
import com.lakony.commandcenter.taskly.TasklySessionStore
import kotlinx.coroutines.launch

private enum class AppDestination(val label: String) {
    Home("Home"), Local("Local"), Taskly("Taskly"), Smith("Smith"), Money("Money"), Settings("Settings")
}

@Composable
fun CommandCenterAppV2() {
    val context = LocalContext.current
    val localStore = remember { LocalStore(context) }
    val tasklyStore = remember { TasklySessionStore(context) }
    val tasklyApi = remember { TasklyApi(tasklyStore) }
    var destination by remember { mutableStateOf(AppDestination.Home) }
    var displayName by remember { mutableStateOf(localStore.loadName()) }
    var tasks by remember { mutableStateOf(localStore.loadTasks()) }

    fun saveTasks(updated: List<AppTask>) {
        tasks = updated
        localStore.saveTasks(updated)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SwissBauhausTopBar(displayName, destination.label) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppDestination.entries.forEach { item ->
                    val icon = when (item) {
                        AppDestination.Home -> Icons.Default.Home
                        AppDestination.Local -> Icons.Default.TaskAlt
                        AppDestination.Taskly -> Icons.Default.CloudSync
                        AppDestination.Smith -> Icons.Default.SmartToy
                        AppDestination.Money -> Icons.Default.AccountBalanceWallet
                        AppDestination.Settings -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 9.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                AppDestination.Home -> BauhausHome(
                    name = displayName,
                    tasks = tasks,
                    tasklyConnected = tasklyStore.isSignedIn,
                    onTaskly = { destination = AppDestination.Taskly },
                    onMoney = { destination = AppDestination.Money },
                    onSmith = { destination = AppDestination.Smith },
                )
                AppDestination.Local -> LocalTasksV2(tasks, ::saveTasks)
                AppDestination.Taskly -> TasklyHubScreen(tasklyStore, tasklyApi)
                AppDestination.Smith -> SmithV2(tasklyStore, tasklyApi, tasks, ::saveTasks) { target ->
                    destination = when (target.lowercase()) {
                        "taskly" -> AppDestination.Taskly
                        "money" -> AppDestination.Money
                        "tasks", "local" -> AppDestination.Local
                        "settings" -> AppDestination.Settings
                        else -> AppDestination.Home
                    }
                }
                AppDestination.Money -> EnhancedMoneyScreen()
                AppDestination.Settings -> EnhancedSettingsScreen(
                    displayName = displayName,
                    onSaveName = {
                        displayName = it
                        localStore.saveName(it)
                    },
                    taskCount = tasks.size,
                    tasklyConnected = tasklyStore.isSignedIn,
                    tasklyUser = tasklyStore.userName,
                    onOpenTaskly = { destination = AppDestination.Taskly },
                )
            }
        }
    }
}

@Composable
private fun SwissBauhausTopBar(name: String, section: String) {
    val context = LocalContext.current
    val profile = remember { ProfileImageStore(context) }
    val bitmap = remember(section, name) { profile.load() }
    Surface(color = MaterialTheme.colorScheme.primary) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(18.dp).background(MaterialTheme.colorScheme.secondary))
                Column {
                    Text("LAKONY / $section", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                    Text("COMMAND CENTER", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            if (bitmap != null) {
                Image(bitmap, "Profile picture", Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun BauhausHome(
    name: String,
    tasks: List<AppTask>,
    tasklyConnected: Boolean,
    onTaskly: () -> Unit,
    onMoney: () -> Unit,
    onSmith: () -> Unit,
) {
    val open = tasks.count { !it.completed }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("HELLO, ${name.uppercase()}", style = MaterialTheme.typography.headlineLarge)
        Text("Function first. Clear hierarchy. Strong geometry.", color = MutedText)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBlock("LOCAL OPEN", open.toString(), Modifier.weight(1f))
            MetricBlock("TASKLY", if (tasklyConnected) "LIVE" else "OFF", Modifier.weight(1f))
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.tertiary))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("TASKLY SYNC", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                    Text(if (tasklyConnected) "Connected" else "Connect your account", style = MaterialTheme.typography.titleLarge)
                    Text("Tasks, workspaces and due reminders.", color = MutedText)
                }
                Button(onClick = onTaskly) { Text("OPEN") }
            }
        }

        Text("MODULES", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleBlock("MONEY", "Balance + plans", Modifier.weight(1f), onMoney)
            ModuleBlock("SMITH", "Commands", Modifier.weight(1f), onSmith)
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
            Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondary))
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, color = MutedText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LocalTasksV2(tasks: List<AppTask>, save: (List<AppTask>) -> Unit) {
    var title by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("LOCAL TASKS", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    save(tasks + AppTask(title = title.trim(), category = "General"))
                    title = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ADD TASK") }
        tasks.forEach { task ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, { checked -> save(tasks.map { if (it.id == task.id) it.copy(completed = checked) else it }) })
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.category, color = MutedText, fontSize = 11.sp)
                    }
                    TextButton(onClick = { save(tasks.filterNot { it.id == task.id }) }) { Text("REMOVE") }
                }
            }
        }
    }
}

@Composable
private fun SmithV2(
    tasklyStore: TasklySessionStore,
    api: TasklyApi,
    tasks: List<AppTask>,
    save: (List<AppTask>) -> Unit,
    navigate: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Ready.") }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SMITH", style = MaterialTheme.typography.headlineMedium)
        Text(if (tasklyStore.isSignedIn) "Taskly commands active." else "Offline commands active.", color = MutedText)
        OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), label = { Text("Command") }, minLines = 2)
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val raw = command.trim()
                val normalized = raw.lowercase().removePrefix("smith,").trim()
                when {
                    normalized == "show my tasks" || normalized == "sync tasks" -> navigate("taskly")
                    normalized.startsWith("add task ") -> {
                        val newTitle = raw.substringAfter("add task ", "", ignoreCase = true).trim()
                        val workspace = tasklyStore.selectedWorkspaceId
                        if (newTitle.isBlank()) response = "Give the task a title."
                        else if (tasklyStore.isSignedIn && workspace != null) {
                            busy = true
                            scope.launch {
                                api.createTask(workspace, newTitle)
                                    .onSuccess { response = "Added to Taskly: ${it.title}" }
                                    .onFailure { response = it.message ?: "Taskly could not add the task." }
                                busy = false
                            }
                        } else {
                            save(tasks + AppTask(title = newTitle, category = "Smith"))
                            response = "Saved locally."
                        }
                    }
                    else -> {
                        val result = SmithCommandEngine.run(raw)
                        response = result.message
                        result.taskToAdd?.let { save(tasks + AppTask(title = it, category = "Smith")) }
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
