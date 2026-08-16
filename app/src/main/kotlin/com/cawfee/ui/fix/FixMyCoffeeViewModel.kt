package com.cawfee.ui.fix

import androidx.lifecycle.ViewModel
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.MachineSettings
import com.cawfee.domain.model.Milk
import com.cawfee.domain.model.MilkKind
import com.cawfee.domain.model.Recommendation
import com.cawfee.domain.model.Symptom
import com.cawfee.domain.rules.RulesEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class FixUiState(
    val drink: DrinkType = DrinkType.FLAT_WHITE,
    val milkKind: MilkKind = MilkKind.DEVONDALE_FULL_CREAM_UHT,
    val settings: MachineSettings = MachineSettings.defaultFlatWhite,
    val selectedSymptoms: Set<Symptom> = emptySet(),
    val recommendation: Recommendation? = null,
)

/** ViewModel for Fix My Coffee / Expert mode, driven entirely by the ported RulesEngine. */
@HiltViewModel
class FixMyCoffeeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(FixUiState())
    val state: StateFlow<FixUiState> = _state.asStateFlow()

    fun setDrink(drink: DrinkType) = _state.update {
        it.copy(drink = drink, settings = MachineSettings.defaults(drink), recommendation = null)
    }

    fun setMilkKind(kind: MilkKind) = _state.update { it.copy(milkKind = kind) }

    fun setSettings(settings: MachineSettings) = _state.update { it.copy(settings = settings) }

    fun toggle(symptom: Symptom) = _state.update { s ->
        val next = if (symptom in s.selectedSymptoms) s.selectedSymptoms - symptom else s.selectedSymptoms + symptom
        s.copy(selectedSymptoms = next)
    }

    fun clearSymptoms() = _state.update { it.copy(selectedSymptoms = emptySet(), recommendation = null) }

    fun evaluate(novice: Boolean) = _state.update { s ->
        val rec = RulesEngine.evaluate(
            symptoms = s.selectedSymptoms.toList(),
            current = s.settings,
            milk = Milk.canonical(s.milkKind),
            drink = s.drink,
            novice = novice,
        )
        s.copy(recommendation = rec)
    }

    fun applyPrimary() = _state.update { s ->
        val primary = s.recommendation?.primary ?: return@update s
        s.copy(settings = primary.apply(s.settings))
    }
}
