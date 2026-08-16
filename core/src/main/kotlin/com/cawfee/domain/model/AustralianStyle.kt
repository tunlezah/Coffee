package com.cawfee.domain.model

/** Australian-style heuristics. Ported from AustralianStyleBias.swift. */
object AustralianStyleBias {
    val preferredTemperature = TemperatureLevel.NORMAL
    val preferredVolumeRange = 30..40
    val preferredMilkSecondsFlatWhite = 14..20
    val preferredMilkSecondsCappuccino = 18..26
    val preferredStrengthRange = 6..8

    fun appliesTo(drink: DrinkType): Boolean =
        drink == DrinkType.FLAT_WHITE || drink == DrinkType.CAPPUCCINO

    fun preferredMilkSeconds(drink: DrinkType): IntRange? = when (drink) {
        DrinkType.FLAT_WHITE -> preferredMilkSecondsFlatWhite
        DrinkType.CAPPUCCINO -> preferredMilkSecondsCappuccino
        else -> null
    }
}

/** Reference drink-style preset. Ported from AustralianStylePreset.swift. */
data class AustralianStylePreset(
    val name: String,
    val drink: DrinkType,
    val ratio: Double,
    val beverageML: Int,
    val milkML: Int,
    val blurb: String,
) {
    companion object {
        val all: List<AustralianStylePreset> = listOf(
            AustralianStylePreset("Ristretto", DrinkType.ESPRESSO, 1.5, 20, 0,
                "A short, syrupy shot — restricted yield for intensity."),
            AustralianStylePreset("Espresso", DrinkType.ESPRESSO, 2.0, 35, 0,
                "The Aussie standard double, ~1:2 in around 30 seconds."),
            AustralianStylePreset("Piccolo", DrinkType.LATTE, 2.0, 90, 55,
                "A single ristretto topped with steamed milk in a small glass."),
            AustralianStylePreset("Flat White", DrinkType.FLAT_WHITE, 2.0, 160, 125,
                "Silky microfoam over a double — the Canberra cafe benchmark."),
            AustralianStylePreset("Cappuccino", DrinkType.CAPPUCCINO, 2.0, 180, 140,
                "More foam than a flat white, dusted with chocolate."),
            AustralianStylePreset("Latte", DrinkType.LATTE, 2.0, 220, 185,
                "Longer milk drink served in a glass."),
            AustralianStylePreset("Long Black", DrinkType.LONG_BLACK, 2.0, 110, 0,
                "A double poured over hot water to preserve crema."),
        )
    }
}
