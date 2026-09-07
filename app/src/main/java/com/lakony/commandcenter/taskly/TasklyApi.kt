package com.lakony.commandcenter.taskly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class TasklyWorkspace(val id: String, val name: String)

data class TasklyTask(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    val workspaceId: String,
    val dueDate: String? = null,
) {
    val completed: Boolean get() = status == "COMPLETED"
}

data class TasklyLoginResult(val userName: String)

class TasklyApi(private val store: TasklySessionStore) {

    suspend fun login(baseUrl: String, email: String, password: String): Result<TasklyLoginResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBase = baseUrl.trim().trimEnd('/')
            require(cleanBase.startsWith("https://") || cleanBase.startsWith("http://")) { "Enter a valid Taskly server URL" }
            val response = requestRaw(
                method = "POST",
                url = "$cleanBase/api/auth/login",
                body = JSONObject().put("email", email.trim()).put("password", password),
                accessToken = null,
            )
            if (response.code !in 200..299) error(response.message())
            val json = JSONObject(response.body)
            store.baseUrl = cleanBase
            store.accessToken = json.optString("accessToken").takeIf { it.isNotBlank() }
            store.refreshToken = json.optString("refreshToken").takeIf { it.isNotBlank() }
            val user = json.optJSONObject("user") ?: JSONObject()
            val first = user.optString("firstname")
            val last = user.optString("lastName")
            val name = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email.substringBefore('@') }
            store.userName = name
            if (!store.isSignedIn) error("Taskly login did not return a complete session")
            TasklyLoginResult(name)
        }
    }

    suspend fun listWorkspaces(): Result<List<TasklyWorkspace>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = authenticatedJson("GET", "/api/workspaces")
            val array = json.optJSONArray("workspaces") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isNotBlank()) add(TasklyWorkspace(id, item.optString("name", "Workspace")))
                }
            }
        }
    }

    suspend fun listTasks(workspaceId: String): Result<List<TasklyTask>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = authenticatedJson("GET", "/api/tasks?workspaceId=${encode(workspaceId)}")
            val array = json.optJSONArray("tasks") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    parseTask(item)?.let(::add)
                }
            }
        }
    }

    suspend fun createTask(workspaceId: String, title: String): Result<TasklyTask> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("title", title.trim())
                .put("workspaceId", workspaceId)
                .put("priority", "MEDIUM")
                .put("status", "TODO")
                .put("clientId", "command-center-${System.currentTimeMillis()}")
            val json = authenticatedJson("POST", "/api/tasks", body)
            parseTask(json.getJSONObject("task")) ?: error("Taskly returned an invalid task")
        }
    }

    suspend fun setCompleted(taskId: String, completed: Boolean): Result<TasklyTask> = withContext(Dispatchers.IO) {
        runCatching {
            val json = authenticatedJson(
                "PATCH",
                "/api/tasks/${encode(taskId)}",
                JSONObject().put("status", if (completed) "COMPLETED" else "TODO"),
            )
            parseTask(json.getJSONObject("task")) ?: error("Taskly returned an invalid task")
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            authenticatedJson("DELETE", "/api/tasks/${encode(taskId)}")
            Unit
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val refresh = store.refreshToken
            if (!refresh.isNullOrBlank() && store.baseUrl.isNotBlank()) {
                requestRaw(
                    "POST",
                    "${store.baseUrl}/api/auth/logout",
                    JSONObject().put("refreshToken", refresh),
                    null,
                )
            }
            store.clearSession()
        }
    }

    private fun parseTask(item: JSONObject): TasklyTask? {
        val id = item.optString("id")
        val title = item.optString("title")
        if (id.isBlank() || title.isBlank()) return null
        return TasklyTask(
            id = id,
            title = title,
            status = item.optString("status", "TODO"),
            priority = item.optString("priority", "MEDIUM"),
            workspaceId = item.optString("workspaceId"),
            dueDate = item.optString("dueDate").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun authenticatedJson(method: String, path: String, body: JSONObject? = null): JSONObject {
        if (store.baseUrl.isBlank()) error("Taskly server URL is not configured")
        var response = requestRaw(method, "${store.baseUrl}$path", body, store.accessToken)
        if (response.code == 401 && refreshAccessToken()) {
            response = requestRaw(method, "${store.baseUrl}$path", body, store.accessToken)
        }
        if (response.code !in 200..299) error(response.message())
        return if (response.body.isBlank()) JSONObject() else JSONObject(response.body)
    }

    private fun refreshAccessToken(): Boolean {
        val refresh = store.refreshToken ?: return false
        val response = requestRaw(
            "POST",
            "${store.baseUrl}/api/auth/refresh",
            JSONObject().put("refreshToken", refresh),
            null,
        )
        if (response.code !in 200..299) {
            store.clearSession()
            return false
        }
        val token = runCatching { JSONObject(response.body).optString("accessToken") }.getOrNull().orEmpty()
        if (token.isBlank()) return false
        store.accessToken = token
        return true
    }

    private fun requestRaw(method: String, url: String, body: JSONObject?, accessToken: String?): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (!accessToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..399) connection.inputStream else connection.errorStream
        val text = if (stream == null) "" else BufferedReader(InputStreamReader(stream)).use { it.readText() }
        connection.disconnect()
        return HttpResponse(code, text)
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    private data class HttpResponse(val code: Int, val body: String) {
        fun message(): String = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty().ifBlank { "Taskly request failed ($code)" }
    }
}
