package com.cawfee.domain

import com.cawfee.domain.model.MachineRanges
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.TemperatureLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from MachineRangesTests.swift. */
class MachineRangesTest {
    @Test fun clampGrinder() {
        assertEquals(1, MachineRanges.clampGrinder(0))
        assertEquals(4, MachineRanges.clampGrinder(4))
        assertEquals(7, MachineRanges.clampGrinder(99))
    }

    @Test fun clampStrength() {
        assertEquals(1, MachineRanges.clampStrength(-1))
        assertEquals(10, MachineRanges.clampStrength(10))
        assertEquals(10, MachineRanges.clampStrength(100))
    }

    @Test fun clampVolume() {
        assertEquals(25, MachineRanges.clampVolume(10))
        assertEquals(240, MachineRanges.clampVolume(300))
    }

    @Test fun clampMilkDuration() {
        assertEquals(3, MachineRanges.clampMilkDuration(0))
        assertEquals(120, MachineRanges.clampMilkDuration(500))
    }

    @Test fun machineSettingsInitClamps() {
        val s = MachineSettings(grinder = 12, strength = -3, volumeML = 5, milkSeconds = 200, temperature = TemperatureLevel.HIGH)
        assertEquals(7, s.grinder)
        assertEquals(1, s.strength)
        assertEquals(25, s.volumeML)
        assertEquals(120, s.milkSeconds)
        assertEquals(TemperatureLevel.HIGH, s.temperature)
    }
}
