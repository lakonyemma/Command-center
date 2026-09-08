package com.lakony.commandcenter.logic

data class SmithResult(
    val message: String,
    val destination: String? = null,
    val taskToAdd: String? = null
)

object SmithCommandEngine {
    fun run(command: String): SmithResult {
        val clean = command.trim()
        val normalized = clean.lowercase()
        if (clean.isBlank()) return SmithResult("Enter a command first.")

        return when {
            normalized.startsWith("add task ") -> {
                val title = clean.drop(9).trim()
                if (title.isBlank()) SmithResult("Tell me the task name.")
                else SmithResult("Task added: $title", destination = "Tasks", taskToAdd = title)
            }
            "opportun" in normalized || "lead" in normalized || "proposal" in normalized || "sales" in normalized -> SmithResult(
                "Opening Smith Mission Control. Review opportunities, approvals, proposals and follow-ups.",
                destination = "Smith"
            )
            "revenue" in normalized || "mrr" in normalized || "customer" in normalized || "invoice" in normalized -> SmithResult(
                "Opening Revenue OS for customers, payments, invoices and recurring revenue.",
                destination = "Revenue"
            )
            "dev" in normalized || "code" in normalized -> SmithResult(
                "Developer mode ready. Open your projects, coding tasks, or debugging work.",
                destination = "Home"
            )
            "school" in normalized || "study" in normalized -> SmithResult(
                "School mode ready. Your academic tasks are the priority.",
                destination = "Tasks"
            )
            "money" in normalized || "finance" in normalized || "budget" in normalized -> SmithResult(
                "Money mode ready. Review your financial snapshot.",
                destination = "Money"
            )
            "brief" in normalized || "today" in normalized -> SmithResult(
                "Smith Mission Control has today's priorities, opportunities and approvals.",
                destination = "Smith"
            )
            "settings" in normalized || "profile" in normalized -> SmithResult(
                "Opening your settings.",
                destination = "Settings"
            )
            else -> SmithResult(
                "I understood the command, but this offline command does not have a matching action yet. Try: brief me, show opportunities, show revenue, dev mode, money mode, or add task followed by a task name."
            )
        }
    }
}
