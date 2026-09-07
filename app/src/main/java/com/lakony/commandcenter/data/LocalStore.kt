package com.lakony.commandcenter.data

import android.content.Context
import com.lakony.commandcenter.model.AppTask

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("lakony_command_center", Context.MODE_PRIVATE)

    fun loadName(): String = prefs.getString("display_name", "Lakony") ?: "Lakony"

    fun saveName(name: String) {
        prefs.edit().putString("display_name", name.trim().ifBlank { "Lakony" }).apply()
    }

    fun loadMoneyIncome(): String = prefs.getString("money_income", "") ?: ""

    fun loadMoneySpending(): String = prefs.getString("money_spending", "") ?: ""

    fun saveMoney(income: String, spending: String) {
        prefs.edit()
            .putString("money_income", income)
            .putString("money_spending", spending)
            .apply()
    }

    fun loadTasks(): List<AppTask> {
        val raw = prefs.getStringSet("tasks", emptySet()).orEmpty()
        return raw.mapNotNull { entry ->
            val parts = entry.split("|", limit = 4)
            if (parts.size != 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val completed = parts[3].toBooleanStrictOrNull() ?: false
            AppTask(id = id, title = decode(parts[1]), category = decode(parts[2]), completed = completed)
        }.sortedBy { it.id }
    }

    fun saveTasks(tasks: List<AppTask>) {
        val encoded = tasks.map { task ->
            "${task.id}|${encode(task.title)}|${encode(task.category)}|${task.completed}"
        }.toSet()
        prefs.edit().putStringSet("tasks", encoded).apply()
    }

    private fun encode(value: String): String = value.replace("%", "%25").replace("|", "%7C")
    private fun decode(value: String): String = value.replace("%7C", "|").replace("%25", "%")
}
