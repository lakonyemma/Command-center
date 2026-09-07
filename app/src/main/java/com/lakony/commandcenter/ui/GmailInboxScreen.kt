package com.lakony.commandcenter.ui

import android.accounts.Account
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.lakony.commandcenter.gmail.GmailClient
import com.lakony.commandcenter.gmail.GmailMessage
import kotlinx.coroutines.launch

private const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
private const val EXPECTED_GMAIL_ACCOUNT = "lakonyemmanuel92@gmail.com"

@Composable
fun GmailInboxScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val client = remember { Identity.getAuthorizationClient(activity) }
    val messages = remember { mutableStateListOf<GmailMessage>() }
    var connectedEmail by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Connect your Google account to load Gmail.") }
    var busy by remember { mutableStateOf(false) }

    val loadInbox: (String) -> Unit = { token ->
        busy = true
        status = "Loading Gmail..."
        scope.launch {
            runCatching { GmailClient.loadInbox(token) }
                .onSuccess { inbox ->
                    if (!inbox.email.equals(EXPECTED_GMAIL_ACCOUNT, ignoreCase = true)) {
                        connectedEmail = inbox.email
                        messages.clear()
                        status = "Wrong Google account. Use $EXPECTED_GMAIL_ACCOUNT."
                    } else {
                        connectedEmail = inbox.email
                        messages.clear()
                        messages.addAll(inbox.messages)
                        status = "Google account connected"
                    }
                }
                .onFailure { error -> status = error.message ?: "Gmail could not be loaded." }
            busy = false
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { client.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { authorization ->
                    val token = authorization.accessToken
                    if (token.isNullOrBlank()) {
                        status = "Google did not return an access token."
                        busy = false
                    } else {
                        loadInbox(token)
                    }
                }
                .onFailure { error ->
                    status = error.message ?: "Google authorization failed."
                    busy = false
                }
        } else {
            status = "Google connection cancelled."
            busy = false
        }
    }

    fun authorize(selectAccount: Boolean) {
        busy = true
        status = if (selectAccount) "Choose your Google account..." else "Connecting $EXPECTED_GMAIL_ACCOUNT..."

        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GMAIL_SCOPE)))

        if (selectAccount) {
            builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
        } else {
            builder.setAccount(Account(EXPECTED_GMAIL_ACCOUNT, "com.google"))
        }

        client.authorize(builder.build())
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) {
                        authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        status = "Google authorization is unavailable."
                        busy = false
                    }
                } else {
                    val token = authorization.accessToken
                    if (token.isNullOrBlank()) {
                        status = "Google did not return an access token."
                        busy = false
                    } else {
                        loadInbox(token)
                    }
                }
            }
            .addOnFailureListener { error ->
                status = if (selectAccount) {
                    error.message ?: "Google authorization failed."
                } else {
                    "Could not use $EXPECTED_GMAIL_ACCOUNT automatically. Tap CHOOSE ACCOUNT below."
                }
                busy = false
            }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
            Column {
                Text("GOOGLE / GMAIL", style = MaterialTheme.typography.headlineMedium)
                Text(EXPECTED_GMAIL_ACCOUNT, color = MutedText)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GOOGLE ACCOUNT", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Text(if (connectedEmail.isBlank()) "Not connected" else connectedEmail, fontWeight = FontWeight.Bold)
                Text(status, color = MutedText, fontSize = 12.sp)
                Button(onClick = { authorize(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (connectedEmail.isBlank()) "CONNECT GOOGLE ACCOUNT" else "REFRESH GMAIL")
                }
                OutlinedButton(onClick = { authorize(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("CHOOSE ACCOUNT")
                }
            }
        }

        if (messages.isNotEmpty()) {
            Text("INBOX", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
            messages.forEach { message ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(message.subject, fontWeight = if (message.unread) FontWeight.Black else FontWeight.Bold)
                        Text(message.from, color = MutedText, fontSize = 12.sp)
                        if (message.snippet.isNotBlank()) Text(message.snippet, fontSize = 12.sp)
                        if (message.date.isNotBlank()) Text(message.date, color = MutedText, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
