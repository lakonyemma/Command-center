package com.lakony.commandcenter.smith

import android.content.Context
import com.lakony.commandcenter.revenue.LeadStage
import com.lakony.commandcenter.revenue.RevenueStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OpportunityStatus {
    NEW,
    REVIEWED,
    APPROVED,
    CONTACTED,
    WON,
    DISMISSED,
}

data class SmithOpportunity(
    val id: String,
    val title: String,
    val company: String,
    val source: String,
    val sourceUrl: String,
    val need: String,
    val estimatedValueUgx: Long,
    val score: Int,
    val status: OpportunityStatus = OpportunityStatus.NEW,
    val proposalDraft: String = "",
    val discoveredAtEpochMs: Long = System.currentTimeMillis(),
)

data class SmithBrief(
    val headline: String,
    val priorities: List<String>,
    val opportunityCount: Int,
    val approvalCount: Int,
)

class SmithRevenueEngine(private val context: Context) {
    private val prefs = context.getSharedPreferences("smith_revenue_engine", Context.MODE_PRIVATE)
    private val revenueStore = RevenueStore(context)

    fun opportunities(): List<SmithOpportunity> = readOpportunities()
        .sortedWith(compareByDescending<SmithOpportunity> { it.score }.thenByDescending { it.discoveredAtEpochMs })

    fun addOpportunity(
        title: String,
        company: String,
        source: String,
        sourceUrl: String,
        need: String,
        estimatedValueUgx: Long,
        score: Int,
    ): SmithOpportunity {
        val opportunity = SmithOpportunity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            company = company.trim(),
            source = source.trim(),
            sourceUrl = sourceUrl.trim(),
            need = need.trim(),
            estimatedValueUgx = estimatedValueUgx.coerceAtLeast(0),
            score = score.coerceIn(0, 100),
        )
        saveOpportunities(readOpportunities() + opportunity)
        return opportunity
    }

    fun updateStatus(id: String, status: OpportunityStatus) {
        saveOpportunities(readOpportunities().map { if (it.id == id) it.copy(status = status) else it })
    }

    fun saveProposal(id: String, draft: String) {
        saveOpportunities(readOpportunities().map { if (it.id == id) it.copy(proposalDraft = draft.trim()) else it })
    }

    fun morningBrief(): SmithBrief {
        val workspace = revenueStore.loadWorkspace()
        val opportunities = opportunities()
        val newHighConfidence = opportunities.count { it.status == OpportunityStatus.NEW && it.score >= 70 }
        val approvals = opportunities.count { it.status == OpportunityStatus.REVIEWED && it.proposalDraft.isNotBlank() }
        val followUps = workspace.leads.count { it.stage in setOf(LeadStage.CONTACTED, LeadStage.QUALIFIED, LeadStage.PROPOSAL) }
        val overdue = workspace.invoices.count { !it.paid && it.dueAtEpochMs < System.currentTimeMillis() }

        val priorities = buildList {
            if (newHighConfidence > 0) add("Review $newHighConfidence high-confidence opportunities Smith found.")
            if (approvals > 0) add("$approvals proposal drafts are waiting for your approval.")
            if (followUps > 0) add("Follow up with $followUps active leads.")
            if (overdue > 0) add("$overdue customer invoices are overdue.")
            if (isEmpty()) add("Pipeline is clear. Focus on finding and qualifying new revenue opportunities.")
        }

        return SmithBrief(
            headline = "Smith morning brief: ${workspace.summary.qualifiedLeads} qualified leads, ${workspace.summary.activeCustomers} active customers, $newHighConfidence new strong opportunities.",
            priorities = priorities.take(4),
            opportunityCount = opportunities.count { it.status !in setOf(OpportunityStatus.WON, OpportunityStatus.DISMISSED) },
            approvalCount = approvals,
        )
    }

    fun eveningBrief(): SmithBrief {
        val workspace = revenueStore.loadWorkspace()
        val opportunities = opportunities()
        val contacted = opportunities.count { it.status == OpportunityStatus.CONTACTED }
        val approvals = opportunities.count { it.status == OpportunityStatus.REVIEWED && it.proposalDraft.isNotBlank() }
        val priorities = buildList {
            if (approvals > 0) add("Approve or reject $approvals prepared proposals before tomorrow.")
            if (contacted > 0) add("Smith is tracking $contacted contacted opportunities for follow-up.")
            if (workspace.summary.outstandingRevenueUgx > 0) add("Outstanding revenue: UGX ${workspace.summary.outstandingRevenueUgx}.")
            add("Tomorrow's first objective: move the highest-value qualified opportunity forward.")
        }
        return SmithBrief(
            headline = "Smith evening review: UGX ${workspace.summary.collectedRevenueUgx} collected, UGX ${workspace.summary.monthlyRecurringRevenueUgx} MRR.",
            priorities = priorities.take(4),
            opportunityCount = opportunities.count { it.status !in setOf(OpportunityStatus.WON, OpportunityStatus.DISMISSED) },
            approvalCount = approvals,
        )
    }

    private fun saveOpportunities(items: List<SmithOpportunity>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("company", item.company)
                put("source", item.source)
                put("sourceUrl", item.sourceUrl)
                put("need", item.need)
                put("estimatedValueUgx", item.estimatedValueUgx)
                put("score", item.score)
                put("status", item.status.name)
                put("proposalDraft", item.proposalDraft)
                put("discoveredAtEpochMs", item.discoveredAtEpochMs)
            })
        }
        prefs.edit().putString("opportunities", array.toString()).apply()
    }

    private fun readOpportunities(): List<SmithOpportunity> {
        val raw = prefs.getString("opportunities", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        SmithOpportunity(
                            id = json.getString("id"),
                            title = json.optString("title"),
                            company = json.optString("company"),
                            source = json.optString("source"),
                            sourceUrl = json.optString("sourceUrl"),
                            need = json.optString("need"),
                            estimatedValueUgx = json.optLong("estimatedValueUgx"),
                            score = json.optInt("score"),
                            status = runCatching { OpportunityStatus.valueOf(json.optString("status", OpportunityStatus.NEW.name)) }.getOrDefault(OpportunityStatus.NEW),
                            proposalDraft = json.optString("proposalDraft"),
                            discoveredAtEpochMs = json.optLong("discoveredAtEpochMs", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
