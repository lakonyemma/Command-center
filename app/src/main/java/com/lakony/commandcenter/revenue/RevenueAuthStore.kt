package com.lakony.commandcenter.revenue

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class RevenueAuthStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "revenue_auth",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveGoogleAccessToken(token: String) {
        prefs.edit()
            .putString("google_access_token", token)
            .putLong("google_access_token_saved_at", System.currentTimeMillis())
            .apply()
    }

    fun googleAccessToken(): String {
        val token = prefs.getString("google_access_token", "").orEmpty()
        val savedAt = prefs.getLong("google_access_token_saved_at", 0L)
        if (token.isBlank() || savedAt == 0L) return ""

        // Google access tokens are short-lived. Do not present an old token as a live cloud session.
        val ageMs = System.currentTimeMillis() - savedAt
        if (ageMs > 50L * 60L * 1000L) {
            clearGoogleAccessToken()
            return ""
        }
        return token
    }

    fun clearGoogleAccessToken() {
        prefs.edit()
            .remove("google_access_token")
            .remove("google_access_token_saved_at")
            .apply()
    }
}
