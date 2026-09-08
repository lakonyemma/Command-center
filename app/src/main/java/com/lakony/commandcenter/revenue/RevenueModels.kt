package com.lakony.commandcenter.revenue

data class RevenueSummary(
    val monthlyRecurringRevenueUgx: Long = 0,
    val collectedRevenueUgx: Long = 0,
    val outstandingRevenueUgx: Long = 0,
    val activeCustomers: Int = 0,
    val activeSubscriptions: Int = 0,
    val qualifiedLeads: Int = 0,
    val conversionRatePercent: Double = 0.0,
)

data class RevenueLead(
    val id: String,
    val name: String,
    val company: String,
    val contact: String,
    val stage: LeadStage = LeadStage.NEW,
    val estimatedMonthlyValueUgx: Long = 0,
)

enum class LeadStage {
    NEW,
    CONTACTED,
    QUALIFIED,
    PROPOSAL,
    WON,
    LOST,
}

data class RevenueCustomer(
    val id: String,
    val name: String,
    val company: String,
    val plan: String,
    val monthlyValueUgx: Long,
    val subscriptionActive: Boolean = true,
)

data class RevenueWorkspace(
    val summary: RevenueSummary = RevenueSummary(),
    val leads: List<RevenueLead> = emptyList(),
    val customers: List<RevenueCustomer> = emptyList(),
)
