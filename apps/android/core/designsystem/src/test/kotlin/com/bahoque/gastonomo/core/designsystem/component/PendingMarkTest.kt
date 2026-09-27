package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * La marca de "sin enviar" de una fila de movimiento.
 *
 * Lleva descripcion de contenido y no solo un icono: quien use lector de pantalla tiene que poder
 * saber que ese gasto todavia no ha salido del telefono.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class PendingMarkTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a movement that has not been sent says so`() {
        compose.setContent { GastonomoTheme { Row(pending = true) } }

        compose.onNodeWithContentDescription(MARK).assertIsDisplayed()
    }

    @Test
    fun `one that has been sent carries no mark`() {
        compose.setContent { GastonomoTheme { Row(pending = false) } }

        assertEquals(0, compose.onAllNodes(hasContentDescription(MARK)).fetchSemanticsNodes().size)
    }

    @androidx.compose.runtime.Composable
    private fun Row(pending: Boolean) {
        MovementRow(
            title = "Mercado",
            subtitle = "Alimentación",
            categoryIcon = "restaurant",
            categoryColor = GastonomoTheme.colors.ink,
            amount = "−$ 186.400",
            amountColor = GastonomoTheme.colors.ink,
            showDivider = false,
            pending = pending,
        )
    }

    private companion object {
        const val MARK = "Sin enviar"
    }
}
