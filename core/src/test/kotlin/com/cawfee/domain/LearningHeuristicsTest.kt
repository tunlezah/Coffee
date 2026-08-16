package com.cawfee.domain

import com.cawfee.domain.model.AdjustmentOutcome
import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.HistorySnapshot
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.rules.LearningHeuristics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ported from LearningHeuristicsTests.swift. */
class LearningHeuristicsTest {

    private fun snap(before: Int, after: Int, outcome: AdjustmentOutcome, dateMillis: Long = 0L) =
        HistorySnapshot(
            dateMillis = dateMillis,
            beforeSettings = MachineSettings(grinder = before),
            afterSettings = MachineSettings(grinder = after),
            symptoms = listOf(Symptom.TOO_BITTER),
            primaryAdjustmentParameter = AdjustmentParameter.GRINDER,
            outcome = outcome,
            drink = DrinkType.FLAT_WHITE,
        )

    @Test fun detectRepeatedFailureRequiresThree() {
        val history = listOf(snap(5, 4, AdjustmentOutcome.UNKNOWN), snap(4, 3, AdjustmentOutcome.UNKNOWN))
        assertFalse(LearningHeuristics.detectRepeatedFailure(history))
    }

    @Test fun detectRepeatedFailureWhenChasingSameDirection() {
        val history = listOf(
            snap(6, 5, AdjustmentOutcome.WORSE),
            snap(5, 4, AdjustmentOutcome.WORSE),
            snap(4, 3, AdjustmentOutcome.UNKNOWN),
        )
        assertTrue(LearningHeuristics.detectRepeatedFailure(history))
    }

    @Test fun recentGoodResetsDetection() {
        val history = listOf(
            snap(6, 5, AdjustmentOutcome.GOOD),
            snap(5, 4, AdjustmentOutcome.WORSE),
            snap(4, 3, AdjustmentOutcome.WORSE),
        )
        assertFalse(LearningHeuristics.detectRepeatedFailure(history))
    }

    @Test fun lastGoodFindsMostRecentInArrayOrder() {
        val history = listOf(
            snap(4, 4, AdjustmentOutcome.GOOD, dateMillis = 1000),
            snap(4, 4, AdjustmentOutcome.WORSE, dateMillis = 2000),
        )
        val lg = LearningHeuristics.lastGood(history)
        assertEquals(1000L, lg?.dateMillis)
    }
}
