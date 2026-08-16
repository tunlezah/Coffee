package com.cawfee.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cawfee.data.PreferencesRepository
import com.cawfee.domain.model.AppearancePreference
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.UserMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val prefs = preferences.prefs

    fun setUserMode(mode: UserMode) = viewModelScope.launch { preferences.setUserMode(mode) }
    fun setAppearance(a: AppearancePreference) = viewModelScope.launch { preferences.setAppearance(a) }
    fun setDefaultDrink(drink: DrinkType) = viewModelScope.launch { preferences.setDefaultDrink(drink) }
}
