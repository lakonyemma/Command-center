package com.lakony.commandcenter.revenue

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

object RevenueApiClient {
    private const val BASE = "https://revenue-os-api-04cu.onrender.com"

    suspend fun loadWorkspace(token: String): RevenueWorkspace = withContext(Dispatchers.IO) {
        val summaryJson = requestObject("GET", "/v1/summary", token)
        val leadsJson = requestArray("GET", "/v1/leads", token)
        val customersJson = requestArray("GET", "/v1/customers", token)
        val paymentsJson = requestArray("GET", "/v1/payments", token)

        RevenueWorkspace(
            summary = RevenueSummary(
                monthlyRecurringRevenueUgx = summaryJson.optLong("monthlyRecurringRevenueUgx"),
                collectedRevenueUgx = summaryJson.optLong("collectedRevenueUgx"),
                outstandingRevenueUgx = summaryJson.optLong("outstandingRevenueUgx"),
                activeCustomers = summaryJson.optInt("activeCustomers"),
                activeSubscriptions = summaryJson.optInt("activeSubscriptions"),
                qualifiedLeads = summaryJson.optInt("qualifiedLeads"),
                conversionRatePercent = summaryJson.optDouble("conversionRatePercent"),
            ),
            leads = buildList {
                for (index in 0 until leadsJson.length()) {
                    val row = leadsJson.getJSONObject(index)
                    add(
                        RevenueLead(
                            id = row.getString("id"),
                            name = row.optString("name"),
                            company = row.optString("company"),
                            contact = row.optString("contact"),
                            stage = runCatching { LeadStage.valueOf(row.optString("stage")) }.getOrDefault(LeadStage.NEW),
                            estimatedMonthlyValueUgx = row.optLong("estimatedMonthlyValueUgx"),
                            createdAtEpochMs = parseInstant(row.optString("createdAt")),
                        ),
                    )
                }
            },
            customers = buildList {
                for (index in 0 until customersJson.length()) {
                    val row = customersJson.getJSONObject(index)
                    add(
                        RevenueCustomer(
                            id = row.getString("id"),
                            name = row.optString("name"),
                            company = row.optString("company"),
                            contact = row.optString("contact"),
                            plan = row.optString("plan", "Custom"),
                            monthlyValueUgx = row.optLong("monthlyValueUgx"),
                            subscriptionActive = row.optBoolean("subscriptionActive", true),
                            createdAtEpochMs = parseInstant(row.optString("createdAt")),
                        ),
                    )
                }
            },
            payments = buildList {
                for (index in 0 until paymentsJson.length()) {
                    val row = paymentsJson.getJSONObject(index)
                    add(
                        RevenuePayment(
                            id = row.getString("id"),
                            customerId = row.optString("customerId"),
                            amountUgx = row.optLong("amountUgx"),
                            status = runCatching { PaymentStatus.valueOf(row.optString("status")) }.getOrDefault(PaymentStatus.PAID),
                            reference = row.optString("reference"),
                            paidAtEpochMs = parseInstant(row.optString("paidAt")),
                        ),
                    )
                }
            },
        )
    }

    suspend fun scanOpportunities(token: String): JSONObject = withContext(Dispatchers.IO) {
        requestObject("POST", "/v1/opportunities/scan", token)
    }

    suspend fun addLead(token: String, name: String, company: String, contact: String, monthlyValueUgx: Long) = withContext(Dispatchers.IO) {
        requestObject(
            "POST", "/v1/leads", token,
            JSONObject().apply {
                put("name", name.trim())
                put("company", company.trim())
                put("contact", contact.trim())
                put("estimated_monthly_value_ugx", monthlyValueUgx.coerceAtLeast(0))
            },
        )
    }

    suspend fun updateLeadStage(token: String, id: String, stage: LeadStage) = withContext(Dispatchers.IO) {
        requestObject("PATCH", "/v1/leads/$id/stage", token, JSONObject().put("stage", stage.name))
    }

    suspend fun addCustomer(token: String, name: String, company: String, contact: String, plan: String, monthlyValueUgx: Long) = withContext(Dispatchers.IO) {
        requestObject(
            "POST", "/v1/customers", token,
            JSONObject().apply {
                put("name", name.trim())
                put("company", company.trim())
                put("contact", contact.trim())
                put("plan", plan.trim().ifBlank { "Custom" })
                put("monthly_value_ugx", monthlyValueUgx)
            },
        )
    }

    suspend fun addPayment(token: String, customerId: String, amountUgx: Long, reference: String) = withContext(Dispatchers.IO) {
        requestObject(
            "POST", "/v1/payments", token,
            JSONObject().apply {
                put("customer_id", customerId)
                put("amount_ugx", amountUgx)
                put("reference", reference.trim())
                put("status", "PAID")
            },
        )
    }

    suspend fun addInvoice(token: String, customerId: String, amountUgx: Long, dueAtEpochMs: Long) = withContext(Dispatchers.IO) {
        requestObject(
            "POST", "/v1/invoices", token,
            JSONObject().apply {
                put("customer_id", customerId)
                put("amount_ugx", amountUgx)
                put("due_at", Instant.ofEpochMilli(dueAtEpochMs).toString())
            },
        )
    }

    private fun requestObject(method: String, path: String, token: String, body: JSONObject? = null): JSONObject {
        val text = request(method, path, token, body)
        return JSONObject(text)
    }

    private fun requestArray(method: String, path: String, token: String): JSONArray {
        return JSONArray(request(method, path, token, null))
    }

    private fun request(method: String, path: String, token: String, body: JSONObject?): String {
        val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) error("Revenue API error $status: $response")
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseInstant(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
