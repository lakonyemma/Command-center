package com.lakony.commandcenter.absa

interface AbsaGateway {
    suspend fun loadFinanceSnapshot(): Result<AbsaFinanceSnapshot>
    val paymentInitiationEnabled: Boolean
}

class MockAbsaGateway : AbsaGateway {
    override val paymentInitiationEnabled: Boolean = false

    override suspend fun loadFinanceSnapshot(): Result<AbsaFinanceSnapshot> = Result.success(
        AbsaFinanceSnapshot(
            account = AbsaAccount(
                accountId = "sandbox-account",
                accountName = "Absa Personal Account",
                maskedAccountNumber = "**** **** 4821",
                availableBalance = 2_400_000,
            ),
            transactions = listOf(
                AbsaTransaction("tx-1", "Salary deposit", 1_800_000, "06 Sep 2026", SpendingCategory.Income),
                AbsaTransaction("tx-2", "Shell Uganda", -120_000, "06 Sep 2026", SpendingCategory.Fuel),
                AbsaTransaction("tx-3", "Glovo", -48_000, "05 Sep 2026", SpendingCategory.Food),
                AbsaTransaction("tx-4", "UMEME", -95_000, "04 Sep 2026", SpendingCategory.Utilities),
            ),
            budgets = listOf(
                SpendingBudget("budget-food", SpendingCategory.Food, 500_000),
                SpendingBudget("budget-fuel", SpendingCategory.Fuel, 300_000),
                SpendingBudget("budget-entertainment", SpendingCategory.Entertainment, 150_000),
            ),
            plannedExpenses = listOf(
                PlannedExpense("plan-rent", "Rent", 800_000, "01 Oct 2026", SpendingCategory.Other),
                PlannedExpense("plan-internet", "Internet", 120_000, "10 Sep 2026", SpendingCategory.Utilities),
            ),
        )
    )
}
