package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.data.LocalStore
import com.lakony.commandcenter.logic.SmithCommandEngine
import com.lakony.commandcenter.model.AppTask

enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Tasks("Tasks", Icons.Default.TaskAlt),
    Smith("Smith", Icons.Default.SmartToy),
    Money("Money", Icons.Default.AccountBalanceWallet),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun CommandCenterApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var destination by remember { mutableStateOf(Destination.Home) }
    var displayName by remember { mutableStateOf(store.loadName()) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }

    fun saveTasks(updated: List<AppTask>) {
        tasks = updated
        store.saveTasks(updated)
    }

    Scaffold(
        containerColor = SurfaceBlue,
        topBar = { AppTopBar(displayName, destination.label) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                Destination.Home -> HomeScreen(displayName, tasks) { destination = it }
                Destination.Tasks -> TasksScreen(
                    tasks = tasks,
                    onAdd = { title, category ->
                        saveTasks(tasks + AppTask(title = title, category = category))
                    },
                    onToggle = { id ->
                        saveTasks(tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it })
                    },
                    onDeleteCompleted = { saveTasks(tasks.filterNot { it.completed }) }
                )
                Destination.Smith -> SmithScreen(
                    onNavigate = { label -> Destination.entries.firstOrNull { it.label == label }?.let { destination = it } },
                    onAddTask = { title -> saveTasks(tasks + AppTask(title = title, category = "Smith")) }
                )
                Destination.Money -> MoneyScreen()
                Destination.Settings -> SettingsScreen(
                    displayName = displayName,
                    onSaveName = {
                        displayName = it
                        store.saveName(it)
                    },
                    taskCount = tasks.size
                )
            }
        }
    }
}

@Composable
private fun AppTopBar(name: String, section: String) {
    Surface(color = DeepBlue) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LAKONY", color = BrightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(section, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Surface(color = PrimaryBlue, shape = RoundedCornerShape(50)) {
                Text(name.take(14), color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(name: String, tasks: List<AppTask>, onNavigate: (Destination) -> Unit) {
    val open = tasks.count { !it.completed }
    val done = tasks.count { it.completed }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Good to see you, $name", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
        Text("Your personal control panel is ready.", color = MutedText)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Open tasks", open.toString(), Modifier.weight(1f))
            StatCard("Completed", done.toString(), Modifier.weight(1f))
        }

        Text("Modes", fontWeight = FontWeight.Bold, color = DeepBlue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard("Developer", Icons.Default.Code, "Code and projects", Modifier.weight(1f)) { onNavigate(Destination.Tasks) }
            ModeCard("School", Icons.Default.School, "Study and revision", Modifier.weight(1f)) { onNavigate(Destination.Tasks) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard("Money", Icons.Default.AccountBalanceWallet, "Budget overview", Modifier.weight(1f)) { onNavigate(Destination.Money) }
            ModeCard("Smith", Icons.Default.SmartToy, "Run commands", Modifier.weight(1f)) { onNavigate(Destination.Smith) }
        }

        Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("TODAY", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (open == 0) "You have no open tasks." else "$open task${if (open == 1) "" else "s"} waiting for you.", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Use Smith to switch modes or add a task quickly.", color = MutedText)
                Button(onClick = { onNavigate(Destination.Smith) }) { Text("Open Smith") }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            Text(label, fontSize = 12.sp, color = MutedText)
        }
    }
}

@Composable
private fun ModeCard(title: String, icon: ImageVector, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue)
            Text(title, fontWeight = FontWeight.Bold, color = DeepBlue)
            Text(subtitle, fontSize = 11.sp, color = MutedText)
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<AppTask>,
    onAdd: (String, String) -> Unit,
    onToggle: (Long) -> Unit,
    onDeleteCompleted: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    val categories = listOf("General", "Developer", "School", "Personal")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tasks", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item, fontSize = 11.sp) })
                    }
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAdd(title.trim(), category)
                            title = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add task")
                }
            }
        }

        if (tasks.isEmpty()) {
            Text("No tasks yet. Add your first one above.", color = MutedText)
        } else {
            tasks.forEach { task ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggle(task.id) }) {
                            Icon(
                                if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (task.completed) "Mark open" else "Mark complete",
                                tint = if (task.completed) PrimaryBlue else MutedText
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                            Text(task.category, fontSize = 11.sp, color = MutedText)
                        }
                    }
                }
            }
            if (tasks.any { it.completed }) {
                TextButton(onClick = onDeleteCompleted, modifier = Modifier.align(Alignment.End)) {
                    Text("Clear completed")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SmithScreen(onNavigate: (String) -> Unit, onAddTask: (String) -> Unit) {
    var command by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Ready for your command.") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Smith", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
        Text("Your offline command layer. More integrations will plug into this screen.", color = MutedText)

        Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Try a command", fontWeight = FontWeight.Bold)
                Text("dev mode • school mode • money mode • brief me • add task Finish database", fontSize = 12.sp, color = MutedText)
            }
        }

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Smith command") },
            placeholder = { Text("Smith, brief me") },
            minLines = 2
        )
        Button(
            onClick = {
                val result = SmithCommandEngine.run(command)
                response = result.message
                result.taskToAdd?.let(onAddTask)
                result.destination?.let(onNavigate)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Run command") }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Smith response", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(response, color = DeepBlue)
            }
        }
    }
}

@Composable
private fun MoneyScreen() {
    var savings by remember { mutableStateOf("") }
    var spending by remember { mutableStateOf("") }
    val balance = (savings.toDoubleOrNull() ?: 0.0) - (spending.toDoubleOrNull() ?: 0.0)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Money", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
        Text("A simple private snapshot. Values stay on this screen only in this version.", color = MutedText)
        OutlinedTextField(value = savings, onValueChange = { savings = it.filter { ch -> ch.isDigit() || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("Savings / income") }, singleLine = true)
        OutlinedTextField(value = spending, onValueChange = { spending = it.filter { ch -> ch.isDigit() || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("Planned spending") }, singleLine = true)
        Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Available balance", fontSize = 12.sp, color = MutedText)
                Text(String.format("%.2f", balance), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun SettingsScreen(displayName: String, onSaveName: (String) -> Unit, taskCount: Int) {
    var name by remember(displayName) { mutableStateOf(displayName) }
    var saved by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
        OutlinedTextField(value = name, onValueChange = { name = it; saved = false }, modifier = Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
        Button(
            onClick = {
                val clean = name.trim().ifBlank { "Lakony" }
                onSaveName(clean)
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save profile") }
        if (saved) Text("Profile saved on this phone.", color = PrimaryBlue)

        HorizontalDivider()
        Text("App status", fontWeight = FontWeight.Bold, color = DeepBlue)
        Text("Local tasks: $taskCount", color = MutedText)
        Text("Theme: Lakony Blue", color = MutedText)
        Text("AI: Offline command engine", color = MutedText)
        Text("Version: 1.0", color = MutedText)
    }
}
