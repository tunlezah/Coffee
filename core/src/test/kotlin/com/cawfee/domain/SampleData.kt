package com.cawfee.domain

import com.cawfee.domain.model.BeanSnapshot
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.RoastLevel

/** Test fixtures mirroring DialedInCoffeeTests/Fixtures/SampleData.swift. */
object SampleData {
    val baselineFlatWhite = MachineSettings(grinder = 4, strength = 7, volumeML = 35, milkSeconds = 18)

    val bean = BeanSnapshot(
        id = "sample-bean",
        name = "Sample Blend",
        roaster = "Test Roastery",
        roastLevel = RoastLevel.MEDIUM,
        milkFriendly = true,
        flavourNotes = listOf("chocolate", "caramel"),
        recommendedSettings = baselineFlatWhite,
    )
}
