package com.lakony.commandcenter.gmail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GmailMessage(
    val id: String,
    val from: String,
    val subject: String,
    val date: String,
    val snippet: String,
    val unread: Boolean,
)

data class GmailInbox(
    val email: String,
    val messages: List<GmailMessage>,
)

object GmailClient {
    private const val BASE = "https://gmail.googleapis.com/gmail/v1/users/me"

    suspend fun loadInbox(accessToken: String): GmailInbox = withContext(Dispatchers.IO) {
        val profile = getJson("$BASE/profile", accessToken)
        val email = profile.optString("emailAddress")

        val list = getJson("$BASE/messages?labelIds=INBOX&maxResults=10", accessToken)
        val refs = list.optJSONArray("messages")
        val messages = buildList {
            if (refs != null) {
                for (index in 0 until refs.length()) {
                    val id = refs.getJSONObject(index).getString("id")
                    val message = getJson(
                        "$BASE/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date",
                        accessToken,
                    )
                    val payload = message.optJSONObject("payload")
                    val headers = payload?.optJSONArray("headers")
                    var from = "Unknown sender"
                    var subject = "(No subject)"
                    var date = ""
                    if (headers != null) {
                        for (headerIndex in 0 until headers.length()) {
                            val header = headers.getJSONObject(headerIndex)
                            when (header.optString("name")) {
                                "From" -> from = header.optString("value", from)
                                "Subject" -> subject = header.optString("value", subject)
                                "Date" -> date = header.optString("value", date)
                            }
                        }
                    }
                    val labelIds = message.optJSONArray("labelIds")
                    var unread = false
                    if (labelIds != null) {
                        for (labelIndex in 0 until labelIds.length()) {
                            if (labelIds.optString(labelIndex) == "UNREAD") unread = true
                        }
                    }
                    add(
                        GmailMessage(
                            id = id,
                            from = from,
                            subject = subject,
                            date = date,
                            snippet = message.optString("snippet"),
                            unread = unread,
                        ),
                    )
                }
            }
        }
        GmailInbox(email = email, messages = messages)
    }

    private fun getJson(url: String, token: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) error("Gmail API error $status: $body")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
