package com.cawfee.domain.rules

import com.cawfee.domain.model.Adjustment
import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.AustralianStyleBias
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineRanges
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.model.TemperatureLevel

/** Translates causes into concrete machine adjustments. Ported from AdjustmentPlanner.swift. */
object AdjustmentPlanner {

    fun adjustment(cause: Cause, current: MachineSettings, drink: DrinkType, milk: Milk): Adjustment? {
        return when (cause) {
            Cause.OVER_EXTRACTION -> {
                val to = MachineRanges.clampGrinder(current.grinder - 1)
                if (to == current.grinder) null
                else Adjustment(parameter = AdjustmentParameter.GRINDER, fromInt = current.grinder, toInt = to,
                    reason = "Coarser grind reduces over-extraction.",
                    expectedOutcome = "Less bitterness, smoother body.")
            }
            Cause.UNDER_EXTRACTION -> {
                val to = MachineRanges.clampGrinder(current.grinder + 1)
                if (to == current.grinder) null
                else Adjustment(parameter = AdjustmentParameter.GRINDER, fromInt = current.grinder, toInt = to,
                    reason = "Finer grind builds extraction.",
                    expectedOutcome = "Less sour, more sweetness and body.")
            }
            Cause.EXCESSIVE_DILUTION -> {
                val target = if (AustralianStyleBias.appliesTo(drink))
                    minOf(current.volumeML - 10, AustralianStyleBias.preferredVolumeRange.last)
                else current.volumeML - 10
                val to = MachineRanges.clampVolume(target)
                if (to == current.volumeML) null
                else Adjustment(parameter = AdjustmentParameter.VOLUME, fromInt = current.volumeML, toInt = to,
                    reason = "Less water concentrates flavour.",
                    expectedOutcome = "Fuller body, more cafe-like cup.")
            }
            Cause.INSUFFICIENT_DILUTION -> {
                val to = MachineRanges.clampVolume(current.volumeML + 5)
                if (to == current.volumeML) null
                else Adjustment(parameter = AdjustmentParameter.VOLUME, fromInt = current.volumeML, toInt = to,
                    reason = "A little more water rounds out the cup.",
                    expectedOutcome = "Less intensity, drinkable balance.")
            }
            Cause.EXCESSIVE_HEAT -> {
                val to = current.temperature.cooler()
                if (to == current.temperature) null
                else Adjustment(parameter = AdjustmentParameter.TEMPERATURE, fromTemp = current.temperature, toTemp = to,
                    reason = "Lower temperature reveals sweetness instead of bitterness.",
                    expectedOutcome = "Smoother, less burnt.")
            }
            Cause.INSUFFICIENT_HEAT -> {
                val to = current.temperature.hotter()
                if (to == current.temperature) null
                else Adjustment(parameter = AdjustmentParameter.TEMPERATURE, fromTemp = current.temperature, toTemp = to,
                    reason = "Warmer brew lifts aromatics.",
                    expectedOutcome = "More fragrance, less sour edge.")
            }
            Cause.EXCESSIVE_STRENGTH -> {
                val to = MachineRanges.clampStrength(current.strength - 1)
                if (to == current.strength) null
                else Adjustment(parameter = AdjustmentParameter.STRENGTH, fromInt = current.strength, toInt = to,
                    reason = "Less coffee for this volume.",
                    expectedOutcome = "More balanced, easier to drink.")
            }
            Cause.INSUFFICIENT_STRENGTH -> {
                val to = MachineRanges.clampStrength(current.strength + 1)
                if (to == current.strength) null
                else Adjustment(parameter = AdjustmentParameter.STRENGTH, fromInt = current.strength, toInt = to,
                    reason = "More coffee for this volume.",
                    expectedOutcome = "Fuller, more coffee-forward.")
            }
            Cause.EXCESSIVE_FOAM -> {
                val to = MachineRanges.clampMilkDuration(current.milkSeconds - 4)
                if (to == current.milkSeconds) null
                else Adjustment(parameter = AdjustmentParameter.MILK_DURATION, fromInt = current.milkSeconds, toInt = to,
                    reason = "Less milk run gives silkier microfoam.",
                    expectedOutcome = "Cafe-style flat-white texture.")
            }
            Cause.STALE_OR_TOO_FINE -> {
                val to = MachineRanges.clampGrinder(current.grinder - 1)
                if (to != current.grinder) {
                    Adjustment(parameter = AdjustmentParameter.GRINDER, fromInt = current.grinder, toInt = to,
                        reason = "A coarser grind helps if flow is choked.",
                        expectedOutcome = "Clearer flavour, less muddy mouthfeel.")
                } else {
                    Adjustment(parameter = AdjustmentParameter.BEANS,
                        reason = "Try fresher beans or a different SKU.",
                        expectedOutcome = "Brighter, more defined flavour.")
                }
            }
            Cause.MILK_OVERWHELMING_COFFEE -> {
                val to = MachineRanges.clampMilkDuration(current.milkSeconds - 4)
                if (to == current.milkSeconds) null
                else Adjustment(parameter = AdjustmentParameter.MILK_DURATION, fromInt = current.milkSeconds, toInt = to,
                    reason = "Less milk lets the espresso speak.",
                    expectedOutcome = "More coffee character in the cup.")
            }
        }
    }

    /** Australian-style override that can pre-empt the raw top cause. */
    fun australianBiasOverride(current: MachineSettings, drink: DrinkType, symptoms: List<Symptom>): Adjustment? {
        if (!AustralianStyleBias.appliesTo(drink)) return null
        val highTempSymptoms = setOf(Symptom.TOO_HOT, Symptom.TOO_BURNT, Symptom.NOT_CAFE_LIKE, Symptom.TOO_BITTER)
        if (current.temperature == TemperatureLevel.HIGH && symptoms.any { it in highTempSymptoms }) {
            return Adjustment(
                parameter = AdjustmentParameter.TEMPERATURE,
                fromTemp = TemperatureLevel.HIGH,
                toTemp = TemperatureLevel.NORMAL,
                reason = "Australian-style milk coffee is smoother at normal temperature.",
                expectedOutcome = "Less burnt, more chocolate-and-caramel character.",
            )
        }
        return null
    }

    fun differentParameter(a: Adjustment, from: Adjustment): Boolean = a.parameter != from.parameter
}
