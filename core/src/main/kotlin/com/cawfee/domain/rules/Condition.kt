package com.cawfee.domain.rules

import com.cawfee.domain.model.BeanSnapshot
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.MilkKind
import com.cawfee.domain.model.RoastLevel
import com.cawfee.domain.model.TemperatureLevel

/** A guard that gates a rule's condition multiplier. Ported from Condition.swift. */
sealed interface Condition {
    data class GrinderAtLeast(val n: Int) : Condition
    data class GrinderAtMost(val n: Int) : Condition
    data class StrengthAtLeast(val n: Int) : Condition
    data class StrengthAtMost(val n: Int) : Condition
    data class VolumeAtLeast(val n: Int) : Condition
    data class VolumeAtMost(val n: Int) : Condition
    data class MilkSecondsAtLeast(val n: Int) : Condition
    data class MilkSecondsAtMost(val n: Int) : Condition
    data class TempEquals(val t: TemperatureLevel) : Condition
    data class DrinkIs(val d: DrinkType) : Condition
    data object DrinkIsMilkBased : Condition
    data class MilkKindIs(val k: MilkKind) : Condition
    data class BeanRoastAtLeast(val r: RoastLevel) : Condition
    data class BeanRoastAtMost(val r: RoastLevel) : Condition

    fun isSatisfied(current: MachineSettings, drink: DrinkType, milk: Milk, bean: BeanSnapshot?): Boolean =
        when (this) {
            is GrinderAtLeast -> current.grinder >= n
            is GrinderAtMost -> current.grinder <= n
            is StrengthAtLeast -> current.strength >= n
            is StrengthAtMost -> current.strength <= n
            is VolumeAtLeast -> current.volumeML >= n
            is VolumeAtMost -> current.volumeML <= n
            is MilkSecondsAtLeast -> current.milkSeconds >= n
            is MilkSecondsAtMost -> current.milkSeconds <= n
            is TempEquals -> current.temperature == t
            is DrinkIs -> drink == d
            DrinkIsMilkBased -> drink.isMilkBased
            is MilkKindIs -> milk.kind == k
            is BeanRoastAtLeast -> bean != null && bean.roastLevel.order >= r.order
            is BeanRoastAtMost -> bean != null && bean.roastLevel.order <= r.order
        }
}
