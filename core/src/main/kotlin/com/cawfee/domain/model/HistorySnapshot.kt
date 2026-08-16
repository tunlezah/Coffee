package com.cawfee.domain.model

import java.util.UUID

/** Outcome of a prior adjustment. Ported from HistorySnapshot.swift. */
enum class AdjustmentOutcome(val displayName: String) {
    UNKNOWN("Not rated"),
    BETTER("Better"),
    WORSE("Worse"),
    GOOD("Good (last good)");
}

/** A historical adjustment record. Ported from HistorySnapshot.swift. */
data class HistorySnapshot(
    val id: UUID = UUID.randomUUID(),
    /** Epoch milliseconds (replaces Swift Date for platform independence). */
    val dateMillis: Long = System.currentTimeMillis(),
    val beforeSettings: MachineSettings,
    val afterSettings: MachineSettings,
    val symptoms: List<Symptom>,
    val primaryAdjustmentParameter: AdjustmentParameter,
    val outcome: AdjustmentOutcome = AdjustmentOutcome.UNKNOWN,
    val beanName: String? = null,
    val drink: DrinkType,
)
