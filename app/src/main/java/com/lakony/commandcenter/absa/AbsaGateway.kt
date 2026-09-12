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
                accountId = "demo-account",
                accountName = "Absa Personal Account",
                maskedAccountNumber = "**** **** 5157",
                availableBalance = 9_324_667,
            ),
            transactions = listOf(
                AbsaTransaction("tx-flight-thu", "Flight ticket", -700_000, "10 Sep 2026", SpendingCategory.Transport),
                AbsaTransaction("tx-flight-tue", "Flight ticket", -700_000, "08 Sep 2026", SpendingCategory.Transport),
            ),
            budgets = listOf(
                SpendingBudget("budget-food", SpendingCategory.Food, 500_000),
                SpendingBudget("budget-fuel", SpendingCategory.Fuel, 300_000),
                SpendingBudget("budget-entertainment", SpendingCategory.Entertainment, 150_000),
            ),
            plannedExpenses = emptyList(),
        )
    )
}
