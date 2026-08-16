package com.cawfee.bluetooth.protocol

import com.cawfee.bluetooth.models.MachineModel
import com.cawfee.bluetooth.models.Product
import com.cawfee.bluetooth.models.ProductSetting
import com.cawfee.bluetooth.models.SettingKind

/**
 * Built-in machine definitions. Currently ships the Jura E8 (model id 15057, group
 * `EF533`), transcribed from the official JOE machine file `EF533/1.0.xml` (the same
 * file the AlexxIT/Jura and Jutta-Proto implementations read at runtime). Designed so
 * additional models can be appended without touching the BLE layers.
 *
 * Setting argument offsets from the XML: strength=F3, water=F4, milk=F5, temperature=F7,
 * milk break=F11. Water is sent as ml/5 (XML @Step=5, "1 second equals 5 ml"); milk and
 * milk break are sent verbatim in SECONDS (XML @Step=1).
 */
object JuraMachineCatalog {

    // Re-usable setting templates for the E8 (EF533) machine file.
    private fun strength(default: Int = 4) =
        ProductSetting(SettingKind.STRENGTH, argument = 3, step = 1, min = 1, max = 8, default = default)

    private fun water(min: Int, max: Int, default: Int) =
        ProductSetting(SettingKind.WATER, argument = 4, step = 5, min = min, max = max, default = default)

    private fun temperature(default: Int = 1) =
        ProductSetting(SettingKind.TEMPERATURE, argument = 7, step = 1, min = 0, max = 2, default = default)

    /** Milk amount is in seconds of milk/foam, sent verbatim (XML @Step=1). */
    private fun milk(default: Int) =
        ProductSetting(SettingKind.MILK, argument = 5, step = 1, min = 3, max = 120, default = default)

    private fun milkBreak(default: Int = 30) =
        ProductSetting(SettingKind.MILK_BREAK, argument = 11, step = 1, min = 0, max = 60, default = default)

    /** Jura E8 — model id 15057, group EF533. Codes/defaults from `EF533/1.0.xml`. */
    val E8: MachineModel = MachineModel(
        modelId = JuraGatt.MODEL_ID_E8,
        name = "E8",
        type = "EF533",
        products = listOf(
            Product(0x01, "Ristretto",
                settings = listOf(strength(), water(15, 80, 25), temperature(default = 2))),
            Product(0x02, "Espresso",
                settings = listOf(strength(), water(15, 80, 45), temperature(default = 2))),
            Product(0x03, "Coffee",
                settings = listOf(strength(), water(25, 240, 100), temperature())),
            Product(0x04, "Cappuccino", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 60), temperature(), milk(default = 14))),
            Product(0x07, "Latte Macchiato", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 45), temperature(default = 2), milk(default = 22), milkBreak())),
            Product(0x0A, "Milk Portion", isMilkBased = true, settings = listOf(milk(default = 30))),
            Product(0x0D, "Hot Water",
                settings = listOf(water(25, 450, 220), temperature())),
            Product(0x11, "2 Ristretti",
                settings = listOf(water(15, 80, 25), temperature(default = 2))),
            Product(0x12, "2 Espressi",
                settings = listOf(water(15, 80, 45), temperature(default = 2))),
            Product(0x13, "2 Coffees",
                settings = listOf(water(25, 240, 100), temperature())),
            Product(0x2E, "Flat White", isMilkBased = true,
                settings = listOf(strength(), water(25, 240, 60), temperature(), milk(default = 14))),
        ),
        // Full E8 alert bit map from the EF533 machine file's <ALERTS> table.
        alertNames = mapOf(
            0 to "Insert tray",
            1 to "Fill water",
            2 to "Empty grounds",
            3 to "Empty tray",
            4 to "Insert coffee bin",
            5 to "Outlet missing",
            6 to "Rear cover missing",
            7 to "Milk alert",
            8 to "Fill system",
            9 to "System filling",
            10 to "No beans",
            11 to "Welcome",
            12 to "Heating up",
            13 to "Coffee ready",
            14 to "No milk (milk sensor)",
            15 to "Error milk (milk sensor)",
            16 to "No signal (milk sensor)",
            17 to "Please wait",
            18 to "Coffee rinsing",
            19 to "Ventilation closed",
            20 to "Close powder cover",
            21 to "Fill powder",
            22 to "System emptying",
            23 to "Not enough powder",
            24 to "Remove water tank",
            25 to "Press rinse",
            26 to "Goodbye",
            27 to "Periphery alert",
            28 to "Powder product",
            29 to "Program mode",
            30 to "Error status",
            31 to "Enjoy product",
            32 to "Filter alert",
            33 to "Descale alert",
            34 to "Cleaning alert",
            35 to "Milk rinse alert",
            36 to "Energy save",
            37 to "Active RF filter",
            38 to "Remote screen",
            39 to "Locked keys",
            40 to "Close tap",
            41 to "Milk clean alert",
            42 to "Info: milk clean",
            43 to "Info: coffee clean",
            44 to "Info: descale",
            45 to "Info: filter used up",
            46 to "Steam ready",
            47 to "Switch-off delay active",
        ),
    )

    private val byId: Map<Int, MachineModel> = listOf(E8).associateBy { it.modelId }

    fun forModelId(modelId: Int): MachineModel? = byId[modelId]

    /** All known models. */
    val all: List<MachineModel> get() = byId.values.toList()
}
