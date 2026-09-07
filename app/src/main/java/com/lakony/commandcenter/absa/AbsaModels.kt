package com.lakony.commandcenter.absa

data class AbsaAccount(
    val accountId: String,
    val accountName: String,
    val maskedAccountNumber: String,
    val currency: String = "UGX",
    val availableBalance: Long,
)

data class AbsaTransaction(
    val id: String,
    val description: String,
    val amount: Long,
    val date: String,
    val category: SpendingCategory,
)

enum class SpendingCategory(val label: String) {
    Food("Food"),
    Transport("Transport"),
    Fuel("Fuel"),
    Utilities("Utilities"),
    Shopping("Shopping"),
    Entertainment("Entertainment"),
    Transfers("Transfers"),
    Income("Income"),
    Other("Other"),
}

data class SpendingBudget(
    val id: String,
    val category: SpendingCategory,
    val limit: Long,
)

data class PlannedExpense(
    val id: String,
    val title: String,
    val amount: Long,
    val dueDate: String,
    val category: SpendingCategory,
)

data class AbsaFinanceSnapshot(
    val account: AbsaAccount,
    val transactions: List<AbsaTransaction>,
    val budgets: List<SpendingBudget>,
    val plannedExpenses: List<PlannedExpense>,
) {
    val upcomingTotal: Long
        get() = plannedExpenses.sumOf { it.amount }

    val projectedBalance: Long
        get() = account.availableBalance - upcomingTotal
}
