package com.cawfee.domain.model

import java.util.UUID

/** One cause's contribution to a recommendation. Ported from Recommendation.swift. */
data class CauseContribution(
    val id: UUID = UUID.randomUUID(),
    val cause: Cause,
    val confidence: Double,          // 0..1
    val ruleIDs: List<String>,
)

/** The full output of the rules engine. Ported from Recommendation.swift. */
data class Recommendation(
    val id: UUID = UUID.randomUUID(),
    val primary: Adjustment? = null,
    val secondary: Adjustment? = null,
    val topCause: Cause? = null,
    val confidence: Double = 0.0,    // 0..1
    val rationale: String = "",
    val contributions: List<CauseContribution> = emptyList(),
    val alternativeCauses: List<CauseContribution> = emptyList(),
    val suggestRevertToLastGood: Boolean = false,
) {
    val adjustments: List<Adjustment> get() = listOfNotNull(primary, secondary)
    val hasAnyAdjustment: Boolean get() = primary != null
}
