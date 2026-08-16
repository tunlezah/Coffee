package com.cawfee.domain.model

/** User experience mode. Ported from UserMode.swift. */
enum class UserMode(val displayName: String) {
    NOVICE("Novice"),
    EXPERT("Expert");
}

/** Appearance preference. Ported from AppearancePreference.swift. */
enum class AppearancePreference(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");
}
