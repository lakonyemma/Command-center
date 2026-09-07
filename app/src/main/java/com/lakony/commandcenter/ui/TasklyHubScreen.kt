package com.lakony.commandcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.taskly.TasklyApi
import com.lakony.commandcenter.taskly.TasklySessionStore
import com.lakony.commandcenter.taskly.TasklyTask
import com.lakony.commandcenter.taskly.TasklyWorkspace
import kotlinx.coroutines.launch

@Composable
fun TasklyHubScreen(store: TasklySessionStore, api: TasklyApi) {
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(store.isSignedIn) }
    var serverUrl by remember { mutableStateOf(store.baseUrl) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(if (signedIn) "Taskly connected." else "Connect your Taskly account.") }
    var workspaces by remember { mutableStateOf<List<TasklyWorkspace>>(emptyList()) }
    var selectedWorkspaceId by remember { mutableStateOf(store.selectedWorkspaceId) }
    var tasks by remember { mutableStateOf<List<TasklyTask>>(emptyList()) }
    var newTask by remember { mutableStateOf("") }

    fun refresh() {
        if (!store.isSignedIn) return
        loading = true
        scope.launch {
            api.listWorkspaces()
                .onSuccess { list ->
                    workspaces = list
                    val selected = selectedWorkspaceId?.takeIf { id -> list.any { it.id == id } }
                        ?: list.firstOrNull()?.id
                    selectedWorkspaceId = selected
                    store.selectedWorkspaceId = selected
                    if (selected == null) {
                        tasks = emptyList()
                        message = "No Taskly workspace found. Create one in Taskly first."
                    } else {
                        api.listTasks(selected)
                            .onSuccess {
                                tasks = it
                                message = "Synced ${it.size} Taskly task${if (it.size == 1) "" else "s"}."
                            }
                            .onFailure { message = it.message ?: "Task sync failed." }
                    }
                }
                .onFailure {
                    signedIn = store.isSignedIn
                    message = it.message ?: "Could not load Taskly workspaces."
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (signedIn) refresh()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Taskly", fontSize = 26.sp, color = DeepBlue)
        Text("Your Command Center now talks directly to the Taskly API.", color = MutedText)

        if (!signedIn) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connect Taskly", color = DeepBlue)
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Taskly server URL") },
                        placeholder = { Text("https://your-taskly-api.onrender.com") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            if (serverUrl.isBlank() || email.isBlank() || password.isBlank()) {
                                message = "Enter the server URL, email, and password."
                                return@Button
                            }
                            loading = true
                            scope.launch {
                                api.login(serverUrl, email, password)
                                    .onSuccess {
                                        signedIn = true
                                        password = ""
                                        message = "Signed in as ${it.userName}."
                                        refresh()
                                    }
                                    .onFailure { message = it.message ?: "Taskly sign-in failed." }
                                loading = false
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (loading) "Connecting…" else "Connect Taskly") }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected", color = PrimaryBlue)
                    Text(store.userName.ifBlank { "Taskly account" }, color = DeepBlue, fontSize = 18.sp)
                    Text(store.baseUrl, color = MutedText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { refresh() }, enabled = !loading) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Sync")
                        }
                        OutlinedButton(onClick = {
                            loading = true
                            scope.launch {
                                api.logout()
                                signedIn = false
                                workspaces = emptyList()
                                tasks = emptyList()
                                message = "Taskly disconnected."
                                loading = false
                            }
                        }) { Text("Disconnect") }
                    }
                }
            }

            if (workspaces.size > 1) {
                Text("Workspace", color = DeepBlue)
                workspaces.forEach { workspace ->
                    FilterChip(
                        selected = selectedWorkspaceId == workspace.id,
                        onClick = {
                            selectedWorkspaceId = workspace.id
                            store.selectedWorkspaceId = workspace.id
                            loading = true
                            scope.launch {
                                api.listTasks(workspace.id)
                                    .onSuccess { tasks = it; message = "Synced ${it.size} tasks from ${workspace.name}." }
                                    .onFailure { message = it.message ?: "Task sync failed." }
                                loading = false
                            }
                        },
                        label = { Text(workspace.name) },
                    )
                }
            }

            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New Taskly task", color = DeepBlue)
                    OutlinedTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Task title") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val workspaceId = selectedWorkspaceId
                            if (workspaceId == null) {
                                message = "Select a workspace first."
                                return@Button
                            }
                            if (newTask.isBlank()) return@Button
                            val title = newTask.trim()
                            loading = true
                            scope.launch {
                                api.createTask(workspaceId, title)
                                    .onSuccess {
                                        tasks = listOf(it) + tasks
                                        newTask = ""
                                        message = "Added to Taskly."
                                    }
                                    .onFailure { message = it.message ?: "Could not create task." }
                                loading = false
                            }
                        },
                        enabled = !loading && newTask.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add to Taskly") }
                }
            }

            Text("Synced tasks", color = DeepBlue)
            if (tasks.isEmpty()) {
                Text("No tasks in this workspace.", color = MutedText)
            }
            tasks.forEach { task ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                api.setCompleted(task.id, !task.completed)
                                    .onSuccess { updated -> tasks = tasks.map { if (it.id == updated.id) updated else it } }
                                    .onFailure { message = it.message ?: "Could not update task." }
                            }
                        }) {
                            Icon(
                                if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (task.completed) "Reopen task" else "Complete task",
                                tint = if (task.completed) PrimaryBlue else MutedText,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = DeepBlue)
                            Text("${task.status.replace('_', ' ')} • ${task.priority}", color = MutedText, fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                api.deleteTask(task.id)
                                    .onSuccess { tasks = tasks.filterNot { it.id == task.id }; message = "Task deleted from Taskly." }
                                    .onFailure { message = it.message ?: "Could not delete task." }
                            }
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete task") }
                    }
                }
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message, color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
    }
}
