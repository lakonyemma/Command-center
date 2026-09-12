package com.lakony.commandcenter.taskly

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TasklySessionStore(context: Context) {
    companion object {
        const val DEFAULT_BASE_URL = "https://taskly-api-wws3.onrender.com"
        private const val SECURE_PREFS = "taskly_secure_session"
    }

    val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences? = createSecurePreferences()

    private fun createSecurePreferences(): SharedPreferences? {
        fun open(): SharedPreferences {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                appContext,
                SECURE_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        return runCatching { open() }
            .recoverCatching {
                appContext.deleteSharedPreferences(SECURE_PREFS)
                open()
            }
            .getOrNull()
    }

    var baseUrl: String
        get() = runCatching { prefs?.getString("base_url", DEFAULT_BASE_URL) }
            .getOrNull()
            ?.ifBlank { DEFAULT_BASE_URL }
            ?: DEFAULT_BASE_URL
        set(value) {
            prefs?.edit()?.putString("base_url", value.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL })?.apply()
        }

    var accessToken: String?
        get() = runCatching { prefs?.getString("access_token", null) }.getOrNull()
        set(value) {
            prefs?.edit()?.putString("access_token", value)?.apply()
        }

    var refreshToken: String?
        get() = runCatching { prefs?.getString("refresh_token", null) }.getOrNull()
        set(value) {
            prefs?.edit()?.putString("refresh_token", value)?.apply()
        }

    var userName: String
        get() = runCatching { prefs?.getString("user_name", "") }.getOrNull() ?: ""
        set(value) {
            prefs?.edit()?.putString("user_name", value)?.apply()
        }

    var selectedWorkspaceId: String?
        get() = runCatching { prefs?.getString("workspace_id", null) }.getOrNull()
        set(value) {
            prefs?.edit()?.putString("workspace_id", value)?.apply()
        }

    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank() && baseUrl.isNotBlank()

    fun clearSession() {
        prefs?.edit()
            ?.remove("access_token")
            ?.remove("refresh_token")
            ?.remove("user_name")
            ?.remove("workspace_id")
            ?.apply()
    }
}
