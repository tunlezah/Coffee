package com.cawfee.domain.rules

import com.cawfee.domain.model.AdjustmentOutcome
import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.HistorySnapshot

/** Detects "chasing your tail" patterns. Ported from LearningHeuristics.swift. */
object LearningHeuristics {

    /**
     * True when there are 3+ recent adjustments to the same parameter in the same
     * direction with no "good" outcome between them.
     */
    fun detectRepeatedFailure(history: List<HistorySnapshot>): Boolean {
        if (history.size < 3) return false
        val recent = history.takeLast(5)

        if (recent.any { it.outcome == AdjustmentOutcome.GOOD }) return false

        val counts = recent.groupingBy { it.primaryAdjustmentParameter }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return false
        if (top.value < 3) return false

        val touches = recent.filter { it.primaryAdjustmentParameter == top.key }
        return monotonicallySameDirection(touches, top.key)
    }

    /** Most recent "good"-marked snapshot in array order, if any. */
    fun lastGood(history: List<HistorySnapshot>): HistorySnapshot? =
        history.asReversed().firstOrNull { it.outcome == AdjustmentOutcome.GOOD }

    private fun monotonicallySameDirection(
        snapshots: List<HistorySnapshot>,
        parameter: AdjustmentParameter,
    ): Boolean {
        var lastDelta: Int? = null
        for (snap in snapshots) {
            val delta = when (parameter) {
                AdjustmentParameter.GRINDER -> snap.afterSettings.grinder - snap.beforeSettings.grinder
                AdjustmentParameter.STRENGTH -> snap.afterSettings.strength - snap.beforeSettings.strength
                AdjustmentParameter.VOLUME -> snap.afterSettings.volumeML - snap.beforeSettings.volumeML
                AdjustmentParameter.MILK_DURATION -> snap.afterSettings.milkSeconds - snap.beforeSettings.milkSeconds
                AdjustmentParameter.TEMPERATURE ->
                    snap.afterSettings.temperature.order - snap.beforeSettings.temperature.order
                AdjustmentParameter.BEANS -> return false
            }
            if (delta == 0) return false
            val last = lastDelta
            if (last != null && (last > 0) != (delta > 0)) return false
            lastDelta = delta
        }
        return lastDelta != null
    }
}
