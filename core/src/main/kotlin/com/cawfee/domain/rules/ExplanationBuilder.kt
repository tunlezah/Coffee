package com.cawfee.domain.rules

import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.Symptom

/** Builds the human-readable rationale. Ported from ExplanationBuilder.swift. */
object ExplanationBuilder {
    fun rationale(topCause: Cause?, symptoms: List<Symptom>, novice: Boolean): String {
        if (topCause == null || symptoms.isEmpty()) {
            return "Couldn't isolate a clear cause from these symptoms. Try a small tweak and re-taste."
        }
        val list = symptoms.map { it.displayName.lowercase() }
        val phrase = when (list.size) {
            1 -> list[0]
            2 -> list.joinToString(" and ")
            else -> {
                val head = list.dropLast(1).joinToString(", ")
                val tail = list.last()
                "$head, and $tail"
            }
        }
        return if (novice) {
            "You said the cup was $phrase. That usually means ${topCause.displayName.lowercase()}."
        } else {
            "Symptoms ($phrase) point to ${topCause.displayName.lowercase()}. ${topCause.plainExplanation}"
        }
    }

    fun ruleSummary(rule: Rule): String =
        "${rule.id} • ${rule.trigger.displayName} → ${rule.cause.displayName} (w=${"%.2f".format(rule.baseWeight)})"
}
