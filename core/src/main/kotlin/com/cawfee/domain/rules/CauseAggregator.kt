package com.cawfee.domain.rules

import com.cawfee.domain.model.BeanSnapshot
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Symptom

/** A cause with its aggregated confidence. Ported from CauseAggregator.swift. */
data class AggregatedCause(
    val cause: Cause,
    val confidence: Double,          // 0..1
    val contributingRuleIDs: List<String>,
)

/**
 * Combines rule weights into per-cause confidence via log-odds: `combined = 1 - Π(1 - wᵢ)`.
 * Monotonic, caps at 1.0, handles overlapping evidence sanely. Ported from
 * CauseAggregator.swift.
 */
object CauseAggregator {
    fun aggregate(
        symptoms: List<Symptom>,
        current: MachineSettings,
        drink: DrinkType,
        milk: Milk,
        bean: BeanSnapshot?,
    ): List<AggregatedCause> {
        val triggered = RuleSet.rules(triggeredBy = symptoms)
        if (triggered.isEmpty()) return emptyList()

        // cause -> (productOfComplements, ruleIDs)
        val byCause = LinkedHashMap<Cause, Pair<Double, MutableList<String>>>()
        for (rule in triggered) {
            val w = rule.effectiveWeight(current, drink, milk, bean)
            val clamped = w.coerceIn(0.0, 1.0)
            val complement = 1.0 - clamped
            val existing = byCause[rule.cause]
            if (existing == null) {
                byCause[rule.cause] = complement to mutableListOf(rule.id)
            } else {
                existing.second.add(rule.id)
                byCause[rule.cause] = (existing.first * complement) to existing.second
            }
        }

        return byCause.map { (cause, value) ->
            AggregatedCause(
                cause = cause,
                confidence = (1.0 - value.first).coerceIn(0.0, 1.0),
                contributingRuleIDs = value.second,
            )
        }.sortedByDescending { it.confidence }
    }
}
