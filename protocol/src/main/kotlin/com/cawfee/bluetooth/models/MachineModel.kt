package com.cawfee.bluetooth.models

/** A configurable setting on a product (§8.1, §11). */
enum class SettingKind { STRENGTH, WATER, TEMPERATURE, MILK, MILK_BREAK }

/** Coffee temperature levels (§11). */
enum class Temperature(val raw: Int) {
    LOW(0x00), NORMAL(0x01), HIGH(0x02);

    companion object {
        fun fromRaw(raw: Int): Temperature = entries.firstOrNull { it.raw == raw } ?: NORMAL
    }
}

/**
 * Definition of one settable parameter of a product. Values are expressed in their
 * natural units (millilitres for [SettingKind.WATER]/[SettingKind.MILK], 1–8 for
 * strength, raw 0/1/2 for temperature). The on-wire byte = value / [step].
 *
 * @param argument the byte offset in the 18-byte start frame (the XML "F"+n value).
 * @param step     the XML @Step; 1 means "store value directly".
 */
data class ProductSetting(
    val kind: SettingKind,
    val argument: Int,
    val step: Int = 1,
    val min: Int = 0,
    val max: Int = 255,
    val default: Int = 0,
) {
    /** Convert a natural-unit value to the on-wire byte (clamped + divided by step). */
    fun toByte(value: Int): Int {
        val clamped = value.coerceIn(min, max)
        return if (step > 1) clamped / step else clamped
    }
}

/** A product the machine can make, identified by its single-byte [code]. */
data class Product(
    val code: Int,
    val name: String,
    val isMilkBased: Boolean = false,
    val settings: List<ProductSetting> = emptyList(),
) {
    fun setting(kind: SettingKind): ProductSetting? = settings.firstOrNull { it.kind == kind }
}

/**
 * A machine definition (the spec's per-model XML, distilled to code). Drives command
 * building, status decoding and statistics indexing.
 */
data class MachineModel(
    val modelId: Int,
    val name: String,
    val type: String,
    val products: List<Product>,
    /** bit index → human-readable alert name. */
    val alertNames: Map<Int, String>,
) {
    fun product(code: Int): Product? = products.firstOrNull { it.code == code }
    fun product(name: String): Product? = products.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
