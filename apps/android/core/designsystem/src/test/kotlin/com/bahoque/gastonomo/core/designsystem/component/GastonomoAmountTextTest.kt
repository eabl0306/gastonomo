package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Un importe se ve con el menos tipografico y se dicta con palabras: un lector de pantalla no
 * sabe leer U+2212 ni los seis puntos de la mascara.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class GastonomoAmountTextTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `an expense is dictated as a negative amount`() {
        render("−$ 186.400")
        compose.onNodeWithContentDescription("menos $ 186.400").assertIsDisplayed()
    }

    @Test
    fun `an income is dictated as a positive amount`() {
        render("+$ 2.800.000")
        compose.onNodeWithContentDescription("más $ 2.800.000").assertIsDisplayed()
    }

    @Test
    fun `a masked amount says that it is hidden and not six dots`() {
        render("$ ••••••")
        compose.onNodeWithContentDescription("importe oculto").assertIsDisplayed()
    }

    @Test
    fun `an amount without a sign is dictated as it is written`() {
        render("$ 186.400")
        compose.onNodeWithContentDescription("$ 186.400").assertIsDisplayed()
    }

    private fun render(text: String) {
        compose.setContent {
            GastonomoTheme { GastonomoAmountText(text) }
        }
    }
}
