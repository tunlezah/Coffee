package com.cawfee.domain.rules

import com.cawfee.domain.model.BeanSnapshot
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Symptom

/** A symptom→cause rule with optional conditional weighting. Ported from Rule.swift. */
data class Rule(
    val id: String,
    val trigger: Symptom,
    val cause: Cause,
    val baseWeight: Double,                 // 0..1 base contribution when trigger fires
    val conditions: List<Condition> = emptyList(),
    val conditionMultiplier: Double = 1.0,  // applied when ALL conditions match
    val rationale: String,
) {
    fun effectiveWeight(current: MachineSettings, drink: DrinkType, milk: Milk, bean: BeanSnapshot?): Double {
        if (conditions.isEmpty()) return baseWeight
        val allMatch = conditions.all { it.isSatisfied(current, drink, milk, bean) }
        return if (allMatch) minOf(1.0, baseWeight * conditionMultiplier) else baseWeight
    }
}
