package com.lakony.commandcenter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakony.commandcenter.github.GitHubClient
import com.lakony.commandcenter.github.GitHubRepository
import kotlinx.coroutines.launch

@Composable
fun GitHubReposScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var repos by remember { mutableStateOf<List<GitHubRepository>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Loading repositories...") }

    fun refresh() {
        loading = true
        status = "Loading repositories..."
        scope.launch {
            runCatching { GitHubClient.loadPublicRepositories() }
                .onSuccess {
                    repos = it
                    status = "${it.size} public repositories loaded"
                }
                .onFailure { error ->
                    status = error.message ?: "Could not load GitHub repositories."
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryBlue)
            Column {
                Text("GITHUB", style = MaterialTheme.typography.headlineMedium)
                Text("lakonyemma • lakonyemmanuel92@gmail.com", color = MutedText, fontSize = 12.sp)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ACCOUNT", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text("@lakonyemma", fontWeight = FontWeight.Bold)
                Text(status, color = MutedText, fontSize = 12.sp)
                Button(onClick = { refresh() }, enabled = !loading) {
                    Text(if (loading) "LOADING…" else "REFRESH REPOS")
                }
            }
        }

        if (repos.isNotEmpty()) {
            Text("REPOSITORIES", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
            repos.forEach { repo ->
                Card(onClick = {
                    if (repo.htmlUrl.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl)))
                    }
                }) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(repo.name, fontWeight = FontWeight.Bold)
                        if (repo.description.isNotBlank()) Text(repo.description, color = MutedText, fontSize = 12.sp)
                        val meta = buildList {
                            if (repo.language.isNotBlank()) add(repo.language)
                            add(if (repo.isPrivate) "Private" else "Public")
                        }.joinToString(" • ")
                        Text(meta, color = PrimaryBlue, fontSize = 11.sp)
                    }
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PRIVATE REPOSITORIES", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(
                    "Public repositories are available now. Private repositories need GitHub OAuth so the app can access them securely without storing a personal access token inside the APK.",
                    color = MutedText,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
