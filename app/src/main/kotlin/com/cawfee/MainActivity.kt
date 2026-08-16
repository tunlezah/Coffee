package com.cawfee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cawfee.data.PreferencesRepository
import com.cawfee.domain.model.AppearancePreference
import com.cawfee.navigation.CawfeeApp
import com.cawfee.ui.theme.CawfeeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by preferences.prefs.collectAsStateWithLifecycle(
                initialValue = PreferencesRepository.UserPrefs()
            )
            val dark = when (prefs.appearance) {
                AppearancePreference.LIGHT -> false
                AppearancePreference.DARK, AppearancePreference.OLED -> true
                AppearancePreference.SYSTEM -> isSystemInDarkTheme()
            }
            CawfeeTheme(
                darkTheme = dark,
                oledBlack = prefs.appearance == AppearancePreference.OLED,
            ) {
                CawfeeApp()
            }
        }
    }
}
