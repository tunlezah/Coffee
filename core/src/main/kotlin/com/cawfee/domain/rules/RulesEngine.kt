package com.cawfee.domain.rules

import com.cawfee.domain.model.Adjustment
import com.cawfee.domain.model.BeanSnapshot
import com.cawfee.domain.model.CauseContribution
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.HistorySnapshot
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Recommendation
import com.cawfee.domain.model.Symptom

/** The orchestrating rules engine. Ported 1:1 from RulesEngine.swift. */
object RulesEngine {

    /** Secondary cause must reach this confidence and target a different parameter. */
    const val SECONDARY_CONFIDENCE_THRESHOLD = 0.6

    fun evaluate(
        symptoms: List<Symptom>,
        current: MachineSettings,
        milk: Milk,
        drink: DrinkType,
        bean: BeanSnapshot? = null,
        recentHistory: List<HistorySnapshot> = emptyList(),
        novice: Boolean = true,
    ): Recommendation {
        // 0. Empty input.
        if (symptoms.isEmpty()) {
            return Recommendation(rationale = "Pick at least one symptom to get a recommendation.")
        }

        // 1. Repeated failure → propose revert to last good.
        if (LearningHeuristics.detectRepeatedFailure(recentHistory)) {
            val lg = LearningHeuristics.lastGood(recentHistory)
            val rationale = if (lg != null) {
                "You've changed the same setting in the same direction several times without success. " +
                    "Consider going back to your last good recipe and changing something else."
            } else {
                "You've been pushing the same setting in one direction. " +
                    "Try the opposite direction or change a different setting instead."
            }
            return Recommendation(
                confidence = 0.8,
                rationale = rationale,
                suggestRevertToLastGood = true,
            )
        }

        // 2. Aggregate causes.
        val aggregated = CauseAggregator.aggregate(symptoms, current, drink, milk, bean)
        val top = aggregated.firstOrNull()
            ?: return Recommendation(rationale = "No matching rules. Try describing the cup differently.")

        // 3. Plan primary (Australian-style override first).
        var primary: Adjustment? = AdjustmentPlanner.australianBiasOverride(current, drink, symptoms)
        if (primary == null) {
            primary = AdjustmentPlanner.adjustment(top.cause, current, drink, milk)
        }

        // 4. Optional secondary — different parameter, above threshold.
        var secondary: Adjustment? = null
        if (primary != null) {
            val runnerUp = aggregated.drop(1).firstOrNull { it.confidence >= SECONDARY_CONFIDENCE_THRESHOLD }
            if (runnerUp != null) {
                val candidate = AdjustmentPlanner.adjustment(runnerUp.cause, current, drink, milk)
                if (candidate != null && AdjustmentPlanner.differentParameter(candidate, primary)) {
                    secondary = candidate
                }
            }
        }

        // 5. Contributions for Expert mode.
        val contributions = aggregated.map {
            CauseContribution(cause = it.cause, confidence = it.confidence, ruleIDs = it.contributingRuleIDs)
        }
        val alternatives = contributions.drop(1).take(3)

        val rationale = ExplanationBuilder.rationale(top.cause, symptoms, novice)

        return Recommendation(
            primary = primary,
            secondary = secondary,
            topCause = top.cause,
            confidence = top.confidence,
            rationale = rationale,
            contributions = contributions,
            alternativeCauses = alternatives,
            suggestRevertToLastGood = false,
        )
    }
}
