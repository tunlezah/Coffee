package com.cawfee.domain.model

/** Drink types supported by the dial-in workflow. Ported from DrinkType.swift. */
enum class DrinkType(val displayName: String, val isMilkBased: Boolean) {
    FLAT_WHITE("Flat White", true),
    CAPPUCCINO("Cappuccino", true),
    LATTE("Latte", true),
    LONG_BLACK("Long Black", false),
    ESPRESSO("Espresso", false);
}

/** Brew temperature level. Ported from TemperatureLevel.swift. */
enum class TemperatureLevel(val displayName: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High");

    fun cooler(): TemperatureLevel = when (this) {
        HIGH -> NORMAL
        NORMAL -> LOW
        LOW -> LOW
    }

    fun hotter(): TemperatureLevel = when (this) {
        LOW -> NORMAL
        NORMAL -> HIGH
        HIGH -> HIGH
    }

    /** Ordinal used for direction comparisons (low < normal < high). */
    val order: Int get() = ordinal
}

/** Milk product. Ported from Milk.swift (MilkKind). */
enum class MilkKind(val displayName: String) {
    DEVONDALE_FULL_CREAM_UHT("Devondale Full Cream UHT"),
    FULL_CREAM_FRESH("Full Cream (Fresh)"),
    SKIM("Skim"),
    LACTOSE_FREE("Lactose-Free"),
    OAT("Oat"),
    SOY("Soy"),
    ALMOND("Almond");
}

/** Roast level with an explicit ordering for condition comparisons. */
enum class RoastLevel(val displayName: String) {
    LIGHT("Light"),
    MEDIUM_LIGHT("Medium-Light"),
    MEDIUM("Medium"),
    MEDIUM_DARK("Medium-Dark"),
    DARK("Dark");

    val order: Int get() = ordinal
}
