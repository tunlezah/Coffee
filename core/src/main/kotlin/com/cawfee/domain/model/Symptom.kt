package com.cawfee.domain.model

/** A cup symptom the user can report. Ported from Symptom.swift. */
enum class Symptom(val displayName: String, val plainExplanation: String) {
    TOO_BITTER("Too bitter", "Harsh, dry, dark-chocolate or burnt-toast feel."),
    TOO_SOUR("Too sour", "Sharp, lemony, vinegary — drink feels green."),
    TOO_WATERY("Too watery", "Thin, dilute, lacks body."),
    TOO_BURNT("Too burnt", "Ashy or scorched, like over-roasted coffee."),
    TOO_DRY("Too dry / harsh", "Astringent, drying on the tongue."),
    TOO_WEAK("Too weak", "Not much coffee flavour — closer to hot milk."),
    TOO_STRONG("Too strong", "Overpowering, intense, hard to drink."),
    TOO_FOAMY("Too foamy", "Too much foam on top, drink feels airy."),
    TOO_HOT("Too hot", "Burns the tongue, masks flavour."),
    TASTES_EMPTY("Tastes empty", "No real flavour — neither bitter nor sweet."),
    NOT_CAFE_LIKE("Not cafe-like", "Just doesn't feel like a cafe coffee."),
    SHARP_AFTERTASTE("Sharp aftertaste", "Lingering harsh or acidic edge."),
    MUDDY_DULL("Muddy / dull", "Flat, heavy, no clarity.");
}
