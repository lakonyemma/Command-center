package com.lakony.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeepBlue = Color(0xFF0B1F3A)
private val PrimaryBlue = Color(0xFF1565C0)
private val BrightBlue = Color(0xFF42A5F5)
private val SoftBlue = Color(0xFFEAF3FF)
private val SurfaceBlue = Color(0xFFF6FAFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LakonyTheme {
                CommandCenterApp()
            }
        }
    }
}

@Composable
private fun LakonyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            secondary = BrightBlue,
            background = SurfaceBlue,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = DeepBlue,
            onSurface = DeepBlue
        ),
        content = content
    )
}

@Composable
private fun CommandCenterApp() {
    var activeMode by remember { mutableStateOf("Command Center") }
    var command by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Ready for your command.") }

    Scaffold(
        containerColor = SurfaceBlue,
        topBar = {
            Surface(color = DeepBlue, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LAKONY", color = BrightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Command Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Surface(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("BLUE MODE", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Good afternoon, Lakony", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            Text("Choose a mode or enter a Smith command.", color = Color(0xFF5B6B7E))

            ModeGrid(activeMode) { activeMode = it; response = "$it mode activated." }
            FocusCard(activeMode)
            QuickActions { response = "$it selected. Add the screenshot, file, or details in chat." }
            CommandBox(command, response, onCommandChange = { command = it }) {
                val normalized = command.trim().lowercase()
                when {
                    "dev" in normalized -> activeMode = "Developer"
                    "school" in normalized || "study" in normalized -> activeMode = "School"
                    "money" in normalized || "finance" in normalized -> activeMode = "Money"
                    "brief" in normalized -> activeMode = "Briefing"
                }
                response = if (command.isBlank()) "Enter a command first." else "Smith received: $command"
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeGrid(activeMode: String, onModeSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Modes", fontWeight = FontWeight.SemiBold, color = DeepBlue)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeButton("Developer", Icons.Default.Code, activeMode == "Developer", Modifier.weight(1f), onModeSelected)
            ModeButton("School", Icons.Default.School, activeMode == "School", Modifier.weight(1f), onModeSelected)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeButton("Money", Icons.Default.AccountBalanceWallet, activeMode == "Money", Modifier.weight(1f), onModeSelected)
            ModeButton("Briefing", Icons.Default.Description, activeMode == "Briefing", Modifier.weight(1f), onModeSelected)
        }
    }
}

@Composable
private fun ModeButton(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onModeSelected: (String) -> Unit) {
    ElevatedButton(
        onClick = { onModeSelected(label) },
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (selected) PrimaryBlue else Color.White,
            contentColor = if (selected) Color.White else DeepBlue
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FocusCard(activeMode: String) {
    val description = when (activeMode) {
        "Developer" -> "GitHub, code, databases, debugging, terminal commands, and project work."
        "School" -> "Assignments, revision, exam preparation, notes, and study sessions."
        "Money" -> "Budgets, savings goals, expenses, business planning, and risk calculations."
        "Briefing" -> "Your priorities, deadlines, projects, messages, and daily overview."
        else -> "Your personal control panel for work, study, planning, and AI actions."
    }
    Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current focus", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Text(activeMode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            Text(description, color = Color(0xFF4F6073))
        }
    }
}

@Composable
private fun QuickActions(onAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick AI actions", fontWeight = FontWeight.SemiBold, color = DeepBlue)
        ActionButton("Explain screenshot", Icons.Default.Description, onAction)
        ActionButton("Fix coding error", Icons.Default.Terminal, onAction)
        ActionButton("Summarize document", Icons.Default.Summarize, onAction)
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onAction: (String) -> Unit) {
    OutlinedButton(
        onClick = { onAction(label) },
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(label)
    }
}

@Composable
private fun CommandBox(command: String, response: String, onCommandChange: (String) -> Unit, onRun: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Smith command", fontWeight = FontWeight.Bold, color = DeepBlue)
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Try: Smith, dev mode") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Button(onClick = onRun, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Run command")
            }
            Text(response, color = Color(0xFF5B6B7E), fontSize = 13.sp)
        }
    }
}
