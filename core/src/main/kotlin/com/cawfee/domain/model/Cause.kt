package com.cawfee.domain.model

/** A diagnosed cause of a cup problem. Ported from Cause.swift. */
enum class Cause(val displayName: String, val plainExplanation: String) {
    OVER_EXTRACTION("Over-extraction", "The grind is finer than the beans want, so water pulls bitter compounds."),
    UNDER_EXTRACTION("Under-extraction", "Water is rushing through too quickly, so the coffee tastes sour and thin."),
    EXCESSIVE_DILUTION("Too much water", "There's more water in the cup than the dose can flavour."),
    INSUFFICIENT_DILUTION("Too little water", "Volume is too small for the dose — the cup tastes intense or syrupy."),
    EXCESSIVE_HEAT("Too hot", "Temperature is masking subtle flavours and burning the tongue."),
    INSUFFICIENT_HEAT("Too cool", "Coffee is dropping below the right serving temperature."),
    EXCESSIVE_STRENGTH("Strength too high", "The strength dial is pulling too much coffee for this volume."),
    INSUFFICIENT_STRENGTH("Strength too low", "Not enough coffee is being ground for the cup size."),
    EXCESSIVE_FOAM("Too much foam", "Milk frothing is producing too much air rather than silky texture."),
    STALE_OR_TOO_FINE("Grind too fine or stale beans", "Beans are losing oils or the grind is choking flow."),
    MILK_OVERWHELMING_COFFEE("Milk overwhelming coffee", "Milk volume or sweetness is drowning the espresso.");
}
