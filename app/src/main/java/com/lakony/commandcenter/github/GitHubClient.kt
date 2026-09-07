package com.lakony.commandcenter.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRepository(
    val name: String,
    val fullName: String,
    val description: String,
    val language: String,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val updatedAt: String,
)

object GitHubClient {
    private const val USERNAME = "lakonyemma"

    suspend fun loadPublicRepositories(): List<GitHubRepository> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/users/$USERNAME/repos?per_page=100&sort=updated"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Lakony-Command-Center")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) error("GitHub API error $status")
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val repo = array.getJSONObject(index)
                    add(
                        GitHubRepository(
                            name = repo.optString("name"),
                            fullName = repo.optString("full_name"),
                            description = repo.optString("description").takeUnless { it == "null" } ?: "",
                            language = repo.optString("language").takeUnless { it == "null" } ?: "",
                            isPrivate = repo.optBoolean("private", false),
                            htmlUrl = repo.optString("html_url"),
                            updatedAt = repo.optString("updated_at"),
                        ),
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
