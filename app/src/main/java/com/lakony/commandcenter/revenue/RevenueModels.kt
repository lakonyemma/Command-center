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
    val createdAtEpochMs: Long = System.currentTimeMillis(),
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
    val contact: String = "",
    val plan: String,
    val monthlyValueUgx: Long,
    val subscriptionActive: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class RevenuePayment(
    val id: String,
    val customerId: String,
    val amountUgx: Long,
    val status: PaymentStatus = PaymentStatus.PAID,
    val reference: String = "",
    val paidAtEpochMs: Long = System.currentTimeMillis(),
)

enum class PaymentStatus {
    PAID,
    PENDING,
    FAILED,
}

data class RevenueInvoice(
    val id: String,
    val customerId: String,
    val amountUgx: Long,
    val dueAtEpochMs: Long,
    val paid: Boolean = false,
)

data class RevenueWorkspace(
    val summary: RevenueSummary = RevenueSummary(),
    val leads: List<RevenueLead> = emptyList(),
    val customers: List<RevenueCustomer> = emptyList(),
    val payments: List<RevenuePayment> = emptyList(),
    val invoices: List<RevenueInvoice> = emptyList(),
)
