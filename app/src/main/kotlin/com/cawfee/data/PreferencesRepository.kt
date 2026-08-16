package com.cawfee.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.cawfee.domain.model.AppearancePreference
import com.cawfee.domain.model.DrinkType
import com.cawfee.domain.model.UserMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** User preferences backed by Jetpack DataStore (replaces the SwiftData singleton). */
@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    data class UserPrefs(
        val userMode: UserMode = UserMode.NOVICE,
        val defaultDrink: DrinkType = DrinkType.FLAT_WHITE,
        val appearance: AppearancePreference = AppearancePreference.SYSTEM,
        val machineName: String = "Jura E8",
        val hasCompletedOnboarding: Boolean = false,
    )

    val prefs: Flow<UserPrefs> = dataStore.data.map { p ->
        UserPrefs(
            userMode = p[KEY_MODE]?.let { runCatching { UserMode.valueOf(it) }.getOrNull() } ?: UserMode.NOVICE,
            defaultDrink = p[KEY_DRINK]?.let { runCatching { DrinkType.valueOf(it) }.getOrNull() } ?: DrinkType.FLAT_WHITE,
            appearance = p[KEY_APPEARANCE]?.let { runCatching { AppearancePreference.valueOf(it) }.getOrNull() } ?: AppearancePreference.SYSTEM,
            machineName = p[KEY_MACHINE] ?: "Jura E8",
            hasCompletedOnboarding = p[KEY_ONBOARDED] ?: false,
        )
    }

    suspend fun setUserMode(mode: UserMode) = dataStore.edit { it[KEY_MODE] = mode.name }
    suspend fun setDefaultDrink(drink: DrinkType) = dataStore.edit { it[KEY_DRINK] = drink.name }
    suspend fun setAppearance(a: AppearancePreference) = dataStore.edit { it[KEY_APPEARANCE] = a.name }
    suspend fun setMachineName(name: String) = dataStore.edit { it[KEY_MACHINE] = name }
    suspend fun setOnboardingComplete(done: Boolean) = dataStore.edit { it[KEY_ONBOARDED] = done }

    private companion object {
        val KEY_MODE = stringPreferencesKey("user_mode")
        val KEY_DRINK = stringPreferencesKey("default_drink")
        val KEY_APPEARANCE = stringPreferencesKey("appearance")
        val KEY_MACHINE = stringPreferencesKey("machine_name")
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
