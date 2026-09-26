package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El encabezado de grupo pone el texto en mayusculas, no el fichero de traduccion. */
@RunWith(AndroidJUnit4::class)
class OverlineTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it renders the label in upper case`() {
        compose.setContent { GastonomoTheme { Overline("Mis categorías") } }
        compose.onNodeWithText("MIS CATEGORÍAS").assertIsDisplayed()
    }
}
