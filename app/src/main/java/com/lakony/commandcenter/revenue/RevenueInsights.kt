package com.lakony.commandcenter.revenue

object RevenueInsights {
    fun recommendations(workspace: RevenueWorkspace): List<String> {
        val actions = mutableListOf<String>()
        val summary = workspace.summary

        val proposalLeads = workspace.leads.filter { it.stage == LeadStage.PROPOSAL }
        val qualifiedLeads = workspace.leads.filter { it.stage == LeadStage.QUALIFIED }
        val newLeads = workspace.leads.filter { it.stage == LeadStage.NEW || it.stage == LeadStage.CONTACTED }

        if (proposalLeads.isNotEmpty()) {
            actions += "Close ${proposalLeads.size} proposal-stage lead${if (proposalLeads.size == 1) "" else "s"}; they are closest to revenue."
        } else if (qualifiedLeads.isNotEmpty()) {
            actions += "Move ${qualifiedLeads.size} qualified lead${if (qualifiedLeads.size == 1) "" else "s"} to a concrete offer or proposal."
        } else if (newLeads.isNotEmpty()) {
            actions += "Follow up ${newLeads.size} early-stage lead${if (newLeads.size == 1) "" else "s"} and qualify their budget and need."
        } else {
            actions += "Pipeline is empty. Add prospects before expecting new revenue."
        }

        if (summary.outstandingRevenueUgx > 0) {
            actions += "Collect outstanding invoices worth UGX ${summary.outstandingRevenueUgx}."
        }

        if (summary.activeCustomers > 0 && summary.collectedRevenueUgx == 0L) {
            actions += "You have active customers but no confirmed payments recorded. Verify and record only real collections."
        }

        if (summary.activeCustomers > 0) {
            val top = workspace.customers.maxByOrNull { it.monthlyValueUgx }
            if (top != null) actions += "Protect ${top.name}; this customer currently has the highest monthly value."
        }

        return actions.take(4)
    }
}
