package com.cawfee.domain

import com.cawfee.domain.model.AdjustmentOutcome
import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.HistorySnapshot
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.model.TemperatureLevel
import com.cawfee.domain.rules.RulesEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ported from RulesEngineTests.swift. */
class RulesEngineTest {
    private val milk = Milk.DEVONDALE_FULL_CREAM_UHT

    @Test fun emptySymptomsProducesNoAdjustment() {
        val rec = RulesEngine.evaluate(emptyList(), SampleData.baselineFlatWhite, milk, DrinkType.FLAT_WHITE)
        assertNull(rec.primary)
        assertNull(rec.secondary)
    }

    @Test fun tooBitterAtFineGrindRecommendsCoarser() {
        val rec = RulesEngine.evaluate(listOf(Symptom.TOO_BITTER), MachineSettings(grinder = 6), milk, DrinkType.FLAT_WHITE)
        assertEquals(Cause.OVER_EXTRACTION, rec.topCause)
        assertEquals(AdjustmentParameter.GRINDER, rec.primary?.parameter)
        assertEquals(6, rec.primary?.fromInt)
        assertEquals(5, rec.primary?.toInt)
        assertTrue(rec.confidence >= 0.7)
    }

    @Test fun tooSourAtCoarseGrindRecommendsFiner() {
        val rec = RulesEngine.evaluate(listOf(Symptom.TOO_SOUR), MachineSettings(grinder = 2), milk, DrinkType.FLAT_WHITE)
        assertEquals(Cause.UNDER_EXTRACTION, rec.topCause)
        assertEquals(AdjustmentParameter.GRINDER, rec.primary?.parameter)
        assertEquals(3, rec.primary?.toInt)
    }

    @Test fun tooWateryRecommendsVolumeReduction() {
        val rec = RulesEngine.evaluate(listOf(Symptom.TOO_WATERY), MachineSettings(volumeML = 120), milk, DrinkType.FLAT_WHITE)
        assertEquals(Cause.EXCESSIVE_DILUTION, rec.topCause)
        assertEquals(AdjustmentParameter.VOLUME, rec.primary?.parameter)
        assertTrue((rec.primary?.toInt ?: 999) < 120)
    }

    @Test fun tooHotFlatWhiteAustralianBiasDropsTemp() {
        val rec = RulesEngine.evaluate(listOf(Symptom.TOO_HOT), MachineSettings(temperature = TemperatureLevel.HIGH), milk, DrinkType.FLAT_WHITE)
        assertEquals(AdjustmentParameter.TEMPERATURE, rec.primary?.parameter)
        assertEquals(TemperatureLevel.NORMAL, rec.primary?.toTemp)
    }

    @Test fun maxTwoAdjustments() {
        val rec = RulesEngine.evaluate(
            listOf(Symptom.TOO_BITTER, Symptom.TOO_BURNT, Symptom.TOO_STRONG),
            MachineSettings(grinder = 6, strength = 9, temperature = TemperatureLevel.HIGH),
            milk, DrinkType.FLAT_WHITE)
        assertNotNull(rec.primary)
        assertTrue(rec.adjustments.size <= 2)
        val s = rec.secondary; val p = rec.primary
        if (s != null && p != null) assertNotEquals(p.parameter, s.parameter)
    }

    @Test fun repeatedFailureSuggestsRevert() {
        val history = (0 until 3).map { i ->
            HistorySnapshot(
                beforeSettings = MachineSettings(grinder = 6 - i),
                afterSettings = MachineSettings(grinder = 5 - i),
                symptoms = listOf(Symptom.TOO_BITTER),
                primaryAdjustmentParameter = AdjustmentParameter.GRINDER,
                outcome = AdjustmentOutcome.WORSE,
                drink = DrinkType.FLAT_WHITE,
            )
        }
        val rec = RulesEngine.evaluate(listOf(Symptom.TOO_BITTER), MachineSettings(grinder = 3), milk, DrinkType.FLAT_WHITE, recentHistory = history)
        assertTrue(rec.suggestRevertToLastGood)
    }

    @Test fun noviceVsExpertRationaleDiffers() {
        val novice = RulesEngine.evaluate(listOf(Symptom.TOO_BITTER), MachineSettings(grinder = 6), milk, DrinkType.FLAT_WHITE, novice = true)
        val expert = RulesEngine.evaluate(listOf(Symptom.TOO_BITTER), MachineSettings(grinder = 6), milk, DrinkType.FLAT_WHITE, novice = false)
        assertNotEquals(novice.rationale, expert.rationale)
    }
}
