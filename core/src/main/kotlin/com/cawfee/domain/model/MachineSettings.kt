package com.cawfee.domain.model

/** Value clamping ranges for the machine. Ported from MachineRanges.swift. */
object MachineRanges {
    val grinderRange = 1..7
    val strengthRange = 1..10
    val volumeRange = 25..240
    val milkDurationRange = 3..120

    fun clampGrinder(value: Int) = value.coerceIn(grinderRange)
    fun clampStrength(value: Int) = value.coerceIn(strengthRange)
    fun clampVolume(value: Int) = value.coerceIn(volumeRange)
    fun clampMilkDuration(value: Int) = value.coerceIn(milkDurationRange)
}

/**
 * Machine settings. Ported from MachineSettings.swift. Construction always clamps to the
 * valid ranges. A private primary constructor stores already-clamped values; the
 * companion `invoke` operator clamps, so `MachineSettings(grinder = 12, ...)` works as in
 * Swift while [copy]/[with] also re-clamp.
 */
class MachineSettings private constructor(
    val grinder: Int,
    val strength: Int,
    val volumeML: Int,
    val milkSeconds: Int,
    val temperature: TemperatureLevel,
) {
    fun with(
        grinder: Int? = null,
        strength: Int? = null,
        volumeML: Int? = null,
        milkSeconds: Int? = null,
        temperature: TemperatureLevel? = null,
    ): MachineSettings = MachineSettings(
        grinder = grinder ?: this.grinder,
        strength = strength ?: this.strength,
        volumeML = volumeML ?: this.volumeML,
        milkSeconds = milkSeconds ?: this.milkSeconds,
        temperature = temperature ?: this.temperature,
    )

    override fun equals(other: Any?): Boolean = other is MachineSettings &&
        grinder == other.grinder && strength == other.strength &&
        volumeML == other.volumeML && milkSeconds == other.milkSeconds &&
        temperature == other.temperature

    override fun hashCode(): Int {
        var r = grinder
        r = 31 * r + strength
        r = 31 * r + volumeML
        r = 31 * r + milkSeconds
        r = 31 * r + temperature.hashCode()
        return r
    }

    override fun toString(): String =
        "MachineSettings(grinder=$grinder, strength=$strength, volumeML=$volumeML, milkSeconds=$milkSeconds, temperature=$temperature)"

    companion object {
        operator fun invoke(
            grinder: Int = 4,
            strength: Int = 7,
            volumeML: Int = 35,
            milkSeconds: Int = 18,
            temperature: TemperatureLevel = TemperatureLevel.NORMAL,
        ): MachineSettings = MachineSettings(
            MachineRanges.clampGrinder(grinder),
            MachineRanges.clampStrength(strength),
            MachineRanges.clampVolume(volumeML),
            MachineRanges.clampMilkDuration(milkSeconds),
            temperature,
        )

        val defaultFlatWhite = MachineSettings(4, 7, 35, 18, TemperatureLevel.NORMAL)
        val defaultCappuccino = MachineSettings(4, 7, 35, 22, TemperatureLevel.NORMAL)

        fun defaults(drink: DrinkType): MachineSettings = when (drink) {
            DrinkType.FLAT_WHITE -> defaultFlatWhite
            DrinkType.CAPPUCCINO -> defaultCappuccino
            DrinkType.LATTE -> MachineSettings(4, 7, 40, 28, TemperatureLevel.NORMAL)
            DrinkType.LONG_BLACK -> MachineSettings(4, 8, 110, 3, TemperatureLevel.NORMAL)
            DrinkType.ESPRESSO -> MachineSettings(4, 8, 35, 3, TemperatureLevel.NORMAL)
        }
    }
}
