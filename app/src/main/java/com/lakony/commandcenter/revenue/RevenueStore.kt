package com.lakony.commandcenter.revenue

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RevenueStore(context: Context) {
    private val prefs = context.getSharedPreferences("revenue_os", Context.MODE_PRIVATE)

    fun loadWorkspace(): RevenueWorkspace {
        val leads = readArray("leads") { json ->
            RevenueLead(
                id = json.getString("id"),
                name = json.getString("name"),
                company = json.optString("company"),
                contact = json.optString("contact"),
                stage = runCatching { LeadStage.valueOf(json.optString("stage", LeadStage.NEW.name)) }.getOrDefault(LeadStage.NEW),
                estimatedMonthlyValueUgx = json.optLong("estimatedMonthlyValueUgx"),
                createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis()),
            )
        }

        val customers = readArray("customers") { json ->
            RevenueCustomer(
                id = json.getString("id"),
                name = json.getString("name"),
                company = json.optString("company"),
                contact = json.optString("contact"),
                plan = json.optString("plan", "Custom"),
                monthlyValueUgx = json.optLong("monthlyValueUgx"),
                subscriptionActive = json.optBoolean("subscriptionActive", true),
                createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis()),
            )
        }

        val payments = readArray("payments") { json ->
            RevenuePayment(
                id = json.getString("id"),
                customerId = json.optString("customerId"),
                amountUgx = json.optLong("amountUgx"),
                status = runCatching { PaymentStatus.valueOf(json.optString("status", PaymentStatus.PAID.name)) }.getOrDefault(PaymentStatus.PAID),
                reference = json.optString("reference"),
                paidAtEpochMs = json.optLong("paidAtEpochMs", System.currentTimeMillis()),
            )
        }

        val invoices = readArray("invoices") { json ->
            RevenueInvoice(
                id = json.getString("id"),
                customerId = json.optString("customerId"),
                amountUgx = json.optLong("amountUgx"),
                dueAtEpochMs = json.optLong("dueAtEpochMs"),
                paid = json.optBoolean("paid", false),
            )
        }

        return buildWorkspace(leads, customers, payments, invoices)
    }

    fun addLead(name: String, company: String, contact: String, monthlyValueUgx: Long): RevenueWorkspace {
        val current = loadWorkspace()
        val lead = RevenueLead(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            company = company.trim(),
            contact = contact.trim(),
            estimatedMonthlyValueUgx = monthlyValueUgx.coerceAtLeast(0),
        )
        saveLeads(current.leads + lead)
        return loadWorkspace()
    }

    fun updateLeadStage(id: String, stage: LeadStage): RevenueWorkspace {
        val current = loadWorkspace()
        saveLeads(current.leads.map { if (it.id == id) it.copy(stage = stage) else it })
        return loadWorkspace()
    }

    fun addCustomer(name: String, company: String, contact: String, plan: String, monthlyValueUgx: Long): RevenueWorkspace {
        val current = loadWorkspace()
        val customer = RevenueCustomer(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            company = company.trim(),
            contact = contact.trim(),
            plan = plan.trim().ifBlank { "Custom" },
            monthlyValueUgx = monthlyValueUgx.coerceAtLeast(0),
        )
        saveCustomers(current.customers + customer)
        return loadWorkspace()
    }

    fun addPayment(customerId: String, amountUgx: Long, reference: String): RevenueWorkspace {
        val current = loadWorkspace()
        val payment = RevenuePayment(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            amountUgx = amountUgx.coerceAtLeast(0),
            status = PaymentStatus.PAID,
            reference = reference.trim(),
        )
        savePayments(current.payments + payment)
        return loadWorkspace()
    }

    fun addInvoice(customerId: String, amountUgx: Long, dueAtEpochMs: Long): RevenueWorkspace {
        val current = loadWorkspace()
        val invoice = RevenueInvoice(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            amountUgx = amountUgx.coerceAtLeast(0),
            dueAtEpochMs = dueAtEpochMs,
        )
        saveInvoices(current.invoices + invoice)
        return loadWorkspace()
    }

    private fun buildWorkspace(
        leads: List<RevenueLead>,
        customers: List<RevenueCustomer>,
        payments: List<RevenuePayment>,
        invoices: List<RevenueInvoice>,
    ): RevenueWorkspace {
        val activeCustomers = customers.filter { it.subscriptionActive }
        val mrr = activeCustomers.sumOf { it.monthlyValueUgx }
        val collected = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amountUgx }
        val outstanding = invoices.filter { !it.paid }.sumOf { it.amountUgx }
        val qualified = leads.count { it.stage in setOf(LeadStage.QUALIFIED, LeadStage.PROPOSAL, LeadStage.WON) }
        val closed = leads.count { it.stage == LeadStage.WON || it.stage == LeadStage.LOST }
        val won = leads.count { it.stage == LeadStage.WON }
        val conversionRate = if (closed == 0) 0.0 else (won.toDouble() / closed.toDouble()) * 100.0

        return RevenueWorkspace(
            summary = RevenueSummary(
                monthlyRecurringRevenueUgx = mrr,
                collectedRevenueUgx = collected,
                outstandingRevenueUgx = outstanding,
                activeCustomers = activeCustomers.size,
                activeSubscriptions = activeCustomers.size,
                qualifiedLeads = qualified,
                conversionRatePercent = conversionRate,
            ),
            leads = leads.sortedByDescending { it.createdAtEpochMs },
            customers = customers.sortedByDescending { it.createdAtEpochMs },
            payments = payments.sortedByDescending { it.paidAtEpochMs },
            invoices = invoices.sortedByDescending { it.dueAtEpochMs },
        )
    }

    private fun saveLeads(items: List<RevenueLead>) = saveArray("leads", items.map { lead ->
        JSONObject().apply {
            put("id", lead.id)
            put("name", lead.name)
            put("company", lead.company)
            put("contact", lead.contact)
            put("stage", lead.stage.name)
            put("estimatedMonthlyValueUgx", lead.estimatedMonthlyValueUgx)
            put("createdAtEpochMs", lead.createdAtEpochMs)
        }
    })

    private fun saveCustomers(items: List<RevenueCustomer>) = saveArray("customers", items.map { customer ->
        JSONObject().apply {
            put("id", customer.id)
            put("name", customer.name)
            put("company", customer.company)
            put("contact", customer.contact)
            put("plan", customer.plan)
            put("monthlyValueUgx", customer.monthlyValueUgx)
            put("subscriptionActive", customer.subscriptionActive)
            put("createdAtEpochMs", customer.createdAtEpochMs)
        }
    })

    private fun savePayments(items: List<RevenuePayment>) = saveArray("payments", items.map { payment ->
        JSONObject().apply {
            put("id", payment.id)
            put("customerId", payment.customerId)
            put("amountUgx", payment.amountUgx)
            put("status", payment.status.name)
            put("reference", payment.reference)
            put("paidAtEpochMs", payment.paidAtEpochMs)
        }
    })

    private fun saveInvoices(items: List<RevenueInvoice>) = saveArray("invoices", items.map { invoice ->
        JSONObject().apply {
            put("id", invoice.id)
            put("customerId", invoice.customerId)
            put("amountUgx", invoice.amountUgx)
            put("dueAtEpochMs", invoice.dueAtEpochMs)
            put("paid", invoice.paid)
        }
    })

    private fun saveArray(key: String, objects: List<JSONObject>) {
        val array = JSONArray()
        objects.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun <T> readArray(key: String, mapper: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(mapper(array.getJSONObject(index)))
                }
            }
        }.getOrDefault(emptyList())
    }
}
