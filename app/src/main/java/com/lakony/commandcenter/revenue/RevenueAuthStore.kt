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
        prefs.edit().putString("google_access_token", token).apply()
    }

    fun googleAccessToken(): String = prefs.getString("google_access_token", "").orEmpty()

    fun clearGoogleAccessToken() {
        prefs.edit().remove("google_access_token").apply()
    }
}
