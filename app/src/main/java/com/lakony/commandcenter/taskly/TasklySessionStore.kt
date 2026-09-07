package com.lakony.commandcenter.taskly

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TasklySessionStore(context: Context) {
    companion object {
        const val DEFAULT_BASE_URL = "https://taskly-api-wws3.onrender.com"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "taskly_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var baseUrl: String
        get() = prefs.getString("base_url", DEFAULT_BASE_URL)?.ifBlank { DEFAULT_BASE_URL } ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString("base_url", value.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL }).apply()

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    var selectedWorkspaceId: String?
        get() = prefs.getString("workspace_id", null)
        set(value) = prefs.edit().putString("workspace_id", value).apply()

    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank() && baseUrl.isNotBlank()

    fun clearSession() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_name")
            .remove("workspace_id")
            .apply()
    }
}
