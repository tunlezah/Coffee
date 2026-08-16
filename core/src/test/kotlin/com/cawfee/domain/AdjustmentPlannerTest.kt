package com.cawfee.domain

import com.cawfee.domain.model.Adjustment
import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.TemperatureLevel
import com.cawfee.domain.rules.AdjustmentPlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ported from AdjustmentPlannerTests.swift. */
class AdjustmentPlannerTest {
    private val milk = Milk.DEVONDALE_FULL_CREAM_UHT

    @Test fun overExtractionCoarsens() {
        val adj = AdjustmentPlanner.adjustment(Cause.OVER_EXTRACTION, MachineSettings(grinder = 5), DrinkType.FLAT_WHITE, milk)
        assertNotNull(adj)
        assertEquals(AdjustmentParameter.GRINDER, adj.parameter)
        assertEquals(5, adj.fromInt)
        assertEquals(4, adj.toInt)
    }

    @Test fun underExtractionFineGrind() {
        val adj = AdjustmentPlanner.adjustment(Cause.UNDER_EXTRACTION, MachineSettings(grinder = 2), DrinkType.FLAT_WHITE, milk)
        assertEquals(AdjustmentParameter.GRINDER, adj?.parameter)
        assertEquals(3, adj?.toInt)
    }

    @Test fun grinderClampedAtLowerBound() {
        val adj = AdjustmentPlanner.adjustment(Cause.OVER_EXTRACTION, MachineSettings(grinder = 1), DrinkType.FLAT_WHITE, milk)
        assertNull(adj)
    }

    @Test fun excessiveHeatCools() {
        val adj = AdjustmentPlanner.adjustment(Cause.EXCESSIVE_HEAT, MachineSettings(temperature = TemperatureLevel.HIGH), DrinkType.FLAT_WHITE, milk)
        assertEquals(AdjustmentParameter.TEMPERATURE, adj?.parameter)
        assertEquals(TemperatureLevel.NORMAL, adj?.toTemp)
    }

    @Test fun australianOverrideOnHighTempMilkDrink() {
        val adj = AdjustmentPlanner.australianBiasOverride(MachineSettings(temperature = TemperatureLevel.HIGH), DrinkType.FLAT_WHITE, listOf(com.cawfee.domain.model.Symptom.TOO_HOT))
        assertNotNull(adj)
        assertEquals(AdjustmentParameter.TEMPERATURE, adj.parameter)
        assertEquals(TemperatureLevel.NORMAL, adj.toTemp)
    }

    @Test fun australianOverrideSkippedForLongBlack() {
        val adj = AdjustmentPlanner.australianBiasOverride(MachineSettings(temperature = TemperatureLevel.HIGH), DrinkType.LONG_BLACK, listOf(com.cawfee.domain.model.Symptom.TOO_HOT))
        assertNull(adj)
    }

    @Test fun differentParameterCheck() {
        val a = Adjustment(parameter = AdjustmentParameter.GRINDER, fromInt = 5, toInt = 4, reason = "", expectedOutcome = "")
        val b = Adjustment(parameter = AdjustmentParameter.STRENGTH, fromInt = 7, toInt = 8, reason = "", expectedOutcome = "")
        val c = Adjustment(parameter = AdjustmentParameter.GRINDER, fromInt = 4, toInt = 3, reason = "", expectedOutcome = "")
        assertTrue(AdjustmentPlanner.differentParameter(a, b))
        assertFalse(AdjustmentPlanner.differentParameter(a, c))
    }
}
