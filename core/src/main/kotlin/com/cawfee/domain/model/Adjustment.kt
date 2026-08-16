package com.cawfee.domain.model

import java.util.UUID

/** Which machine parameter an adjustment targets. Ported from Adjustment.swift. */
enum class AdjustmentParameter { GRINDER, STRENGTH, VOLUME, MILK_DURATION, TEMPERATURE, BEANS }

/** A concrete recommended change to the machine. Ported from Adjustment.swift. */
data class Adjustment(
    val id: UUID = UUID.randomUUID(),
    val parameter: AdjustmentParameter,
    val fromInt: Int? = null,
    val toInt: Int? = null,
    val fromTemp: TemperatureLevel? = null,
    val toTemp: TemperatureLevel? = null,
    val reason: String,
    val expectedOutcome: String,
) {
    val summary: String
        get() = when (parameter) {
            AdjustmentParameter.GRINDER -> "Grinder: ${fromInt ?: 0} → ${toInt ?: 0}"
            AdjustmentParameter.STRENGTH -> "Strength: ${fromInt ?: 0} → ${toInt ?: 0}"
            AdjustmentParameter.VOLUME -> "Volume: ${fromInt ?: 0}ml → ${toInt ?: 0}ml"
            AdjustmentParameter.MILK_DURATION -> "Milk: ${fromInt ?: 0}s → ${toInt ?: 0}s"
            AdjustmentParameter.TEMPERATURE -> "Temperature: ${fromTemp?.displayName ?: "?"} → ${toTemp?.displayName ?: "?"}"
            AdjustmentParameter.BEANS -> "Consider a fresher / different bean."
        }

    /** Direction: -1 down, 0 same, +1 up; null when not applicable (beans). */
    val direction: Int?
        get() = when (parameter) {
            AdjustmentParameter.GRINDER, AdjustmentParameter.STRENGTH,
            AdjustmentParameter.VOLUME, AdjustmentParameter.MILK_DURATION -> {
                val f = fromInt; val t = toInt
                if (f == null || t == null) 0 else t.compareTo(f).coerceIn(-1, 1)
            }
            AdjustmentParameter.TEMPERATURE -> {
                val f = fromTemp; val t = toTemp
                if (f == null || t == null) 0 else t.order.compareTo(f.order).coerceIn(-1, 1)
            }
            AdjustmentParameter.BEANS -> null
        }

    fun apply(settings: MachineSettings): MachineSettings = when (parameter) {
        AdjustmentParameter.GRINDER -> settings.with(grinder = toInt ?: settings.grinder)
        AdjustmentParameter.STRENGTH -> settings.with(strength = toInt ?: settings.strength)
        AdjustmentParameter.VOLUME -> settings.with(volumeML = toInt ?: settings.volumeML)
        AdjustmentParameter.MILK_DURATION -> settings.with(milkSeconds = toInt ?: settings.milkSeconds)
        AdjustmentParameter.TEMPERATURE -> settings.with(temperature = toTemp ?: settings.temperature)
        AdjustmentParameter.BEANS -> settings
    }
}
