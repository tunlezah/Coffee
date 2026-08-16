package com.cawfee.domain

import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.model.TemperatureLevel
import com.cawfee.domain.rules.CauseAggregator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ported from CauseAggregatorTests.swift. */
class CauseAggregatorTest {
    private val milk = Milk.DEVONDALE_FULL_CREAM_UHT

    @Test fun logOddsMonotonic() {
        val single = CauseAggregator.aggregate(
            listOf(Symptom.TOO_BITTER), SampleData.baselineFlatWhite, DrinkType.FLAT_WHITE, milk, SampleData.bean)
        val multi = CauseAggregator.aggregate(
            listOf(Symptom.TOO_BITTER, Symptom.TOO_BURNT),
            SampleData.baselineFlatWhite.with(temperature = TemperatureLevel.HIGH),
            DrinkType.FLAT_WHITE, milk, SampleData.bean)

        val single1 = single.firstOrNull { it.cause == Cause.OVER_EXTRACTION || it.cause == Cause.EXCESSIVE_HEAT }
        val multi1 = multi.firstOrNull()
        assertTrue(single1 != null && multi1 != null)
        assertTrue(multi1.confidence >= single1.confidence - 0.0001)
        assertTrue(multi1.confidence <= 1.0)
    }

    @Test fun aggregateEmptyForNoSymptoms() {
        val result = CauseAggregator.aggregate(emptyList(), SampleData.baselineFlatWhite, DrinkType.FLAT_WHITE, milk, null)
        assertTrue(result.isEmpty())
    }

    @Test fun confidenceBounded() {
        val result = CauseAggregator.aggregate(
            Symptom.entries.toList(), SampleData.baselineFlatWhite, DrinkType.FLAT_WHITE, milk, SampleData.bean)
        assertFalse(result.isEmpty())
        for (c in result) {
            assertTrue(c.confidence in 0.0..1.0)
        }
    }
}
