package com.cawfee

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cawfee.ui.misc.PlaceholderScreen
import com.cawfee.ui.theme.CawfeeTheme
import org.junit.Rule
import org.junit.Test

/** Compose UI test exercising a screen end-to-end on device/emulator. */
class PlaceholderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsScreenTitle() {
        composeRule.setContent {
            CawfeeTheme(dynamicColor = false) {
                PlaceholderScreen(title = "Beans")
            }
        }
        composeRule.onNodeWithText("Beans").assertIsDisplayed()
    }
}
