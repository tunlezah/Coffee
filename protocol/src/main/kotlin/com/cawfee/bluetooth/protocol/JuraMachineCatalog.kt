package com.cawfee.bluetooth.protocol

import com.cawfee.bluetooth.models.MachineModel
import com.cawfee.bluetooth.models.Product
import com.cawfee.bluetooth.models.ProductSetting
import com.cawfee.bluetooth.models.SettingKind

/**
 * Built-in machine definitions. Currently ships the Jura E8 (`EF533`) table distilled
 * from §11 of the protocol spec. Designed so additional models can be appended without
 * touching the BLE layers ("future command expansion").
 *
 * Setting argument offsets (§8.1, §11): strength=F3, water=F4, temperature=F7,
 * milk=F5, milk_break=F11. Water is stored as ml/5 (XML @Step=5).
 */
object JuraMachineCatalog {

    // Re-usable setting templates for the E8 family.
    private fun strength(default: Int = 4) =
        ProductSetting(SettingKind.STRENGTH, argument = 3, step = 1, min = 1, max = 8, default = default)

    private fun water(min: Int, max: Int, default: Int) =
        ProductSetting(SettingKind.WATER, argument = 4, step = 5, min = min, max = max, default = default)

    private fun temperature(default: Int = 1) =
        ProductSetting(SettingKind.TEMPERATURE, argument = 7, step = 1, min = 0, max = 2, default = default)

    private fun milk(min: Int = 0, max: Int = 120, default: Int = 0) =
        ProductSetting(SettingKind.MILK, argument = 5, step = 5, min = min, max = max, default = default)

    private fun milkBreak(default: Int = 0) =
        ProductSetting(SettingKind.MILK_BREAK, argument = 11, step = 1, min = 0, max = 60, default = default)

    /** Jura E8 — model id 15057, type EF533. */
    val E8: MachineModel = MachineModel(
        modelId = JuraGatt.MODEL_ID_E8,
        name = "E8",
        type = "EF533",
        products = listOf(
            Product(0x01, "Ristretto", settings = listOf(strength(), water(15, 80, 20), temperature())),
            Product(0x02, "Espresso", settings = listOf(strength(), water(15, 80, 45), temperature())),
            Product(0x03, "Coffee", settings = listOf(strength(), water(25, 240, 110), temperature())),
            Product(0x04, "Cappuccino", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 60), temperature(), milk(default = 60))),
            Product(0x07, "Latte Macchiato", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 60), temperature(), milk(default = 100), milkBreak())),
            Product(0x0A, "Milk Portion", isMilkBased = true, settings = listOf(milk(default = 60))),
            Product(0x0D, "Hot Water", settings = listOf(water(25, 450, 150), temperature(default = 0))),
            Product(0x11, "2 Ristretti", settings = listOf(water(15, 80, 20), temperature())),
            Product(0x12, "2 Espressi", settings = listOf(water(15, 80, 45), temperature())),
            Product(0x13, "2 Coffees", settings = listOf(water(25, 240, 110), temperature())),
            Product(0x2E, "Flat White", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 60), temperature(), milk(default = 40))),
        ),
        // E8 / E-series alert bit map (§9).
        alertNames = mapOf(
            0 to "Insert/empty tray missing",
            1 to "Fill water",
            2 to "Empty grounds",
            3 to "Empty tray",
            4 to "Insert coffee bin",
            5 to "Outlet missing",
            6 to "Rear cover missing",
            7 to "Milk alert",
            10 to "No beans",
            13 to "Coffee ready",
            32 to "Filter alert",
            33 to "Descale alert",
            34 to "Cleaning alert",
        ),
    )

    private val byId: Map<Int, MachineModel> = listOf(E8).associateBy { it.modelId }

    fun forModelId(modelId: Int): MachineModel? = byId[modelId]

    /** All known models. */
    val all: List<MachineModel> get() = byId.values.toList()
}
