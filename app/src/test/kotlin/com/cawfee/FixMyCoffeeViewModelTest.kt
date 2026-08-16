package com.cawfee

import com.cawfee.domain.model.AdjustmentParameter
import com.cawfee.domain.model.Cause
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Symptom
import com.cawfee.ui.fix.FixMyCoffeeViewModel
import com.cawfee.ui.shots.ShotTimerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM unit tests for the Android ViewModels that contain no Android framework calls. */
class FixMyCoffeeViewModelTest {

    @Test
    fun `evaluating too bitter at fine grind recommends coarser grind`() {
        val vm = FixMyCoffeeViewModel()
        vm.setSettings(MachineSettings(grinder = 6))
        vm.toggle(Symptom.TOO_BITTER)
        vm.evaluate(novice = true)

        val rec = vm.state.value.recommendation
        assertTrue(rec != null)
        assertEquals(Cause.OVER_EXTRACTION, rec!!.topCause)
        assertEquals(AdjustmentParameter.GRINDER, rec.primary?.parameter)
        assertEquals(5, rec.primary?.toInt)
    }

    @Test
    fun `clearing symptoms removes the recommendation`() {
        val vm = FixMyCoffeeViewModel()
        vm.toggle(Symptom.TOO_SOUR)
        vm.evaluate(novice = true)
        assertTrue(vm.state.value.recommendation != null)
        vm.clearSymptoms()
        assertNull(vm.state.value.recommendation)
        assertTrue(vm.state.value.selectedSymptoms.isEmpty())
    }

    @Test
    fun `shot timer formats milliseconds as tenths of a second`() {
        assertEquals("27.4", ShotTimerViewModel.format(27_400))
        assertEquals("0.0", ShotTimerViewModel.format(0))
    }
}
