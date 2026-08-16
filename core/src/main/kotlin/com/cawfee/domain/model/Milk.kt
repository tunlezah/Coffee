package com.cawfee.domain.model

/** Milk properties that inform rule evaluation. Ported from Milk.swift. */
data class Milk(
    val kind: MilkKind,
    val sweetness: Double,            // 0..1
    val bodyWeight: Double,           // 0..1 (perceived body / fat)
    val foamability: Double,          // 0..1
    val perceivedBitterness: Double,  // 0..1, how much it amplifies bitterness
) {
    companion object {
        val DEVONDALE_FULL_CREAM_UHT = Milk(MilkKind.DEVONDALE_FULL_CREAM_UHT, 0.65, 0.75, 0.7, 0.35)
        val FULL_CREAM_FRESH = Milk(MilkKind.FULL_CREAM_FRESH, 0.7, 0.8, 0.85, 0.3)
        val SKIM = Milk(MilkKind.SKIM, 0.55, 0.35, 0.9, 0.55)
        val LACTOSE_FREE = Milk(MilkKind.LACTOSE_FREE, 0.8, 0.7, 0.75, 0.3)
        val OAT = Milk(MilkKind.OAT, 0.6, 0.65, 0.6, 0.5)
        val SOY = Milk(MilkKind.SOY, 0.5, 0.55, 0.55, 0.6)
        val ALMOND = Milk(MilkKind.ALMOND, 0.4, 0.35, 0.4, 0.7)

        fun canonical(kind: MilkKind): Milk = when (kind) {
            MilkKind.DEVONDALE_FULL_CREAM_UHT -> DEVONDALE_FULL_CREAM_UHT
            MilkKind.FULL_CREAM_FRESH -> FULL_CREAM_FRESH
            MilkKind.SKIM -> SKIM
            MilkKind.LACTOSE_FREE -> LACTOSE_FREE
            MilkKind.OAT -> OAT
            MilkKind.SOY -> SOY
            MilkKind.ALMOND -> ALMOND
        }

        val allCanonical: List<Milk> = MilkKind.entries.map { canonical(it) }
    }
}
