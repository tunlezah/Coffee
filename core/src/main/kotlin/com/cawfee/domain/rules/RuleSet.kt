package com.cawfee.domain.rules

import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.RoastLevel
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.model.TemperatureLevel

/** The full rule base, ported 1:1 from RuleSet.swift. */
object RuleSet {
    val allRules: List<Rule> = listOf(
        // --- Too Bitter ---
        Rule("bitter.overExtraction.base", Symptom.TOO_BITTER, Cause.OVER_EXTRACTION, 0.7,
            rationale = "Bitterness usually means water is pulling too much from the grounds."),
        Rule("bitter.overExtraction.fineGrind", Symptom.TOO_BITTER, Cause.OVER_EXTRACTION, 0.55,
            conditions = listOf(Condition.GrinderAtLeast(5)), conditionMultiplier = 1.2,
            rationale = "Grinder is on the fine side — that exacerbates over-extraction."),
        Rule("bitter.excessiveHeat", Symptom.TOO_BITTER, Cause.EXCESSIVE_HEAT, 0.35,
            conditions = listOf(Condition.TempEquals(TemperatureLevel.HIGH)), conditionMultiplier = 1.4,
            rationale = "Temperature set to high tends to accentuate bitter notes."),
        Rule("bitter.darkRoast", Symptom.TOO_BITTER, Cause.EXCESSIVE_STRENGTH, 0.3,
            conditions = listOf(Condition.BeanRoastAtLeast(RoastLevel.MEDIUM_DARK), Condition.StrengthAtLeast(8)),
            conditionMultiplier = 1.3,
            rationale = "Dark roast with high strength pulls extra bitterness."),

        // --- Too Sour ---
        Rule("sour.underExtraction.base", Symptom.TOO_SOUR, Cause.UNDER_EXTRACTION, 0.75,
            rationale = "Sourness usually means water isn't pulling enough — grind finer."),
        Rule("sour.underExtraction.coarse", Symptom.TOO_SOUR, Cause.UNDER_EXTRACTION, 0.55,
            conditions = listOf(Condition.GrinderAtMost(3)), conditionMultiplier = 1.25,
            rationale = "Grinder is coarse — likely the dominant cause."),
        Rule("sour.insufficientHeat", Symptom.TOO_SOUR, Cause.INSUFFICIENT_HEAT, 0.3,
            conditions = listOf(Condition.TempEquals(TemperatureLevel.LOW)), conditionMultiplier = 1.3,
            rationale = "Low temperature can leave acidity in the cup."),

        // --- Too Watery ---
        Rule("watery.excessiveDilution.base", Symptom.TOO_WATERY, Cause.EXCESSIVE_DILUTION, 0.75,
            rationale = "Volume is overpowering the dose — cut volume."),
        Rule("watery.excessiveDilution.largeVolume", Symptom.TOO_WATERY, Cause.EXCESSIVE_DILUTION, 0.55,
            conditions = listOf(Condition.VolumeAtLeast(80)), conditionMultiplier = 1.3,
            rationale = "Volume is well above an Aus-style flat white."),
        Rule("watery.insufficientStrength", Symptom.TOO_WATERY, Cause.INSUFFICIENT_STRENGTH, 0.4,
            conditions = listOf(Condition.StrengthAtMost(5)), conditionMultiplier = 1.3,
            rationale = "Strength is low — more coffee would help body."),

        // --- Too Burnt ---
        Rule("burnt.excessiveHeat.base", Symptom.TOO_BURNT, Cause.EXCESSIVE_HEAT, 0.7,
            rationale = "Burnt taste correlates strongly with temperature being too high."),
        Rule("burnt.excessiveHeat.highTemp", Symptom.TOO_BURNT, Cause.EXCESSIVE_HEAT, 0.55,
            conditions = listOf(Condition.TempEquals(TemperatureLevel.HIGH)), conditionMultiplier = 1.4,
            rationale = "Temperature is already high — drop it."),
        Rule("burnt.overExtraction", Symptom.TOO_BURNT, Cause.OVER_EXTRACTION, 0.35,
            conditions = listOf(Condition.GrinderAtLeast(6)), conditionMultiplier = 1.2,
            rationale = "Grinder is very fine, adding ashy notes."),

        // --- Too Dry / Harsh ---
        Rule("dry.overExtraction", Symptom.TOO_DRY, Cause.OVER_EXTRACTION, 0.6,
            rationale = "Astringency points to over-extraction — coarsen the grind."),
        Rule("dry.staleOrTooFine", Symptom.TOO_DRY, Cause.STALE_OR_TOO_FINE, 0.35,
            conditions = listOf(Condition.GrinderAtLeast(6)), conditionMultiplier = 1.2,
            rationale = "Very fine grind can dry the palate."),

        // --- Too Weak ---
        Rule("weak.insufficientStrength.base", Symptom.TOO_WEAK, Cause.INSUFFICIENT_STRENGTH, 0.7,
            rationale = "Increase strength to get more coffee into the cup."),
        Rule("weak.excessiveDilution", Symptom.TOO_WEAK, Cause.EXCESSIVE_DILUTION, 0.4,
            conditions = listOf(Condition.VolumeAtLeast(50)), conditionMultiplier = 1.3,
            rationale = "Volume is high relative to dose — shorten the pour."),
        Rule("weak.milkOverwhelming", Symptom.TOO_WEAK, Cause.MILK_OVERWHELMING_COFFEE, 0.45,
            conditions = listOf(Condition.DrinkIsMilkBased, Condition.MilkSecondsAtLeast(25)),
            conditionMultiplier = 1.3,
            rationale = "Milk run is long — it's drowning the espresso."),

        // --- Too Strong ---
        Rule("strong.excessiveStrength.base", Symptom.TOO_STRONG, Cause.EXCESSIVE_STRENGTH, 0.7,
            rationale = "Strength is too high for this cup."),
        Rule("strong.insufficientDilution", Symptom.TOO_STRONG, Cause.INSUFFICIENT_DILUTION, 0.35,
            conditions = listOf(Condition.VolumeAtMost(30)), conditionMultiplier = 1.3,
            rationale = "Volume is at the low end — a touch more water helps."),

        // --- Too Foamy ---
        Rule("foamy.excessiveFoam.base", Symptom.TOO_FOAMY, Cause.EXCESSIVE_FOAM, 0.75,
            rationale = "Reduce milk duration for less aerated foam."),
        Rule("foamy.excessiveFoam.long", Symptom.TOO_FOAMY, Cause.EXCESSIVE_FOAM, 0.55,
            conditions = listOf(Condition.MilkSecondsAtLeast(28)), conditionMultiplier = 1.3,
            rationale = "Milk frothing time is long."),

        // --- Too Hot ---
        Rule("hot.excessiveHeat.base", Symptom.TOO_HOT, Cause.EXCESSIVE_HEAT, 0.8,
            rationale = "Drop the brew temperature."),
        Rule("hot.excessiveHeat.high", Symptom.TOO_HOT, Cause.EXCESSIVE_HEAT, 0.6,
            conditions = listOf(Condition.TempEquals(TemperatureLevel.HIGH)), conditionMultiplier = 1.4,
            rationale = "Temperature is set to high."),

        // --- Tastes Empty ---
        Rule("empty.insufficientStrength", Symptom.TASTES_EMPTY, Cause.INSUFFICIENT_STRENGTH, 0.55,
            rationale = "Lift strength to give the cup body and flavour."),
        Rule("empty.underExtraction", Symptom.TASTES_EMPTY, Cause.UNDER_EXTRACTION, 0.4,
            conditions = listOf(Condition.GrinderAtMost(3)), conditionMultiplier = 1.2,
            rationale = "Coarse grind is producing a hollow cup."),
        Rule("empty.milkOverwhelming", Symptom.TASTES_EMPTY, Cause.MILK_OVERWHELMING_COFFEE, 0.35,
            conditions = listOf(Condition.DrinkIsMilkBased, Condition.MilkSecondsAtLeast(24)),
            conditionMultiplier = 1.3,
            rationale = "Milk is masking the coffee."),

        // --- Not Cafe-Like ---
        Rule("notCafeLike.excessiveDilution", Symptom.NOT_CAFE_LIKE, Cause.EXCESSIVE_DILUTION, 0.45,
            conditions = listOf(Condition.VolumeAtLeast(60)), conditionMultiplier = 1.3,
            rationale = "Cafe-style flat whites sit around 30–40ml espresso."),
        Rule("notCafeLike.excessiveHeat", Symptom.NOT_CAFE_LIKE, Cause.EXCESSIVE_HEAT, 0.4,
            conditions = listOf(Condition.TempEquals(TemperatureLevel.HIGH)), conditionMultiplier = 1.3,
            rationale = "Cafe-style milk drinks aren't scalding."),
        Rule("notCafeLike.excessiveFoam", Symptom.NOT_CAFE_LIKE, Cause.EXCESSIVE_FOAM, 0.4,
            conditions = listOf(Condition.DrinkIs(com.cawfee.domain.model.DrinkType.FLAT_WHITE), Condition.MilkSecondsAtLeast(22)),
            conditionMultiplier = 1.3,
            rationale = "A flat white should be silky, not airy."),

        // --- Sharp Aftertaste ---
        Rule("sharp.overExtraction", Symptom.SHARP_AFTERTASTE, Cause.OVER_EXTRACTION, 0.5,
            rationale = "A harsh finish usually means over-extraction."),
        Rule("sharp.underExtraction", Symptom.SHARP_AFTERTASTE, Cause.UNDER_EXTRACTION, 0.45,
            conditions = listOf(Condition.GrinderAtMost(3)), conditionMultiplier = 1.2,
            rationale = "Coarse grind can give a thin, acidic finish."),

        // --- Muddy / Dull ---
        Rule("muddy.staleOrTooFine.base", Symptom.MUDDY_DULL, Cause.STALE_OR_TOO_FINE, 0.6,
            rationale = "A muddy cup often means stale beans or a choked grind."),
        Rule("muddy.staleOrTooFine.fineGrind", Symptom.MUDDY_DULL, Cause.STALE_OR_TOO_FINE, 0.5,
            conditions = listOf(Condition.GrinderAtLeast(6)), conditionMultiplier = 1.3,
            rationale = "Grinder is very fine — flow is being choked."),
        Rule("muddy.overExtraction", Symptom.MUDDY_DULL, Cause.OVER_EXTRACTION, 0.35,
            rationale = "Over-extraction can flatten clarity."),
    )

    fun rules(triggeredBy: List<Symptom>): List<Rule> {
        val set = triggeredBy.toSet()
        return allRules.filter { it.trigger in set }
    }
}
