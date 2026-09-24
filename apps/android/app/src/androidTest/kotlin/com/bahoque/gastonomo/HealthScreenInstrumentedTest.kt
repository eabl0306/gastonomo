package com.bahoque.gastonomo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.feature.health.HealthScreenTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de UI en dispositivo o emulador. Arranca la app real y comprueba que la pantalla
 * de salud se pinta y ofrece el boton de reintentar.
 */
@RunWith(AndroidJUnit4::class)
class HealthScreenInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun healthScreenIsDisplayed() {
        composeRule.onNodeWithTag(HealthScreenTags.ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(HealthScreenTags.REFRESH).assertIsDisplayed()
    }
}
