package com.lakony.commandcenter.money

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PlannedSpend(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double,
    val dueAtMillis: Long,
    val completed: Boolean = false,
)

class MoneyStore(context: Context) {
    private val prefs = context.getSharedPreferences("command_center_money", Context.MODE_PRIVATE)

    var manualBalance: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("manual_balance", java.lang.Double.doubleToRawLongBits(0.0)))
        set(value) = prefs.edit().putLong("manual_balance", java.lang.Double.doubleToRawLongBits(value)).apply()

    var absaConsentStatus: String
        get() = prefs.getString("absa_consent_status", "NOT_CONNECTED") ?: "NOT_CONNECTED"
        set(value) = prefs.edit().putString("absa_consent_status", value).apply()

    fun loadPlannedSpends(): List<PlannedSpend> {
        val raw = prefs.getString("planned_spends", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        PlannedSpend(
                            id = item.optLong("id"),
                            title = item.optString("title"),
                            amount = item.optDouble("amount"),
                            dueAtMillis = item.optLong("dueAtMillis"),
                            completed = item.optBoolean("completed"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePlannedSpends(items: List<PlannedSpend>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("amount", item.amount)
                    .put("dueAtMillis", item.dueAtMillis)
                    .put("completed", item.completed)
            )
        }
        prefs.edit().putString("planned_spends", array.toString()).apply()
    }
}
