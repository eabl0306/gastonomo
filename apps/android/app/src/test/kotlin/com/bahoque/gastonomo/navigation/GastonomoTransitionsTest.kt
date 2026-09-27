package com.bahoque.gastonomo.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.AppContainer
import com.bahoque.gastonomo.GastonomoApp
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Que cada movimiento signifique una cosa distinta, que es lo unico que no se puede romper: si
 * cambiar de tab se animara igual que entrar en un detalle, el gesto de volver dejaria de ser
 * predecible.
 *
 * Los dos ultimos tests no miran la animacion, que no se puede asertar: comprueban que con las
 * cuatro transiciones cableadas la navegacion sigue llegando a donde tiene que llegar, ida y
 * vuelta. Es lo que se rompe si una transicion se queda a medias.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rCO-w390dp-h844dp-xxhdpi")
class GastonomoTransitionsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `changing tab does not slide`() {
        assertEquals(NavMotion.FADE, navMotionOf(ScreenKind.TAB, ScreenKind.TAB))
    }

    @Test
    fun `stacking a screen and coming back slide horizontally`() {
        assertEquals(NavMotion.PUSH, navMotionOf(ScreenKind.TAB, ScreenKind.PUSHED))
        assertEquals(NavMotion.PUSH, navMotionOf(ScreenKind.PUSHED, ScreenKind.TAB))
        assertEquals(NavMotion.PUSH, navMotionOf(ScreenKind.PUSHED, ScreenKind.PUSHED))
    }

    @Test
    fun `a creation form moves vertically in both directions`() {
        assertEquals(NavMotion.SHEET, navMotionOf(ScreenKind.TAB, ScreenKind.FORM))
        assertEquals(NavMotion.SHEET, navMotionOf(ScreenKind.FORM, ScreenKind.TAB))
        assertEquals(NavMotion.SHEET, navMotionOf(ScreenKind.PUSHED, ScreenKind.FORM))
    }

    @Test
    fun `the four screens with a tab bar are tabs`() {
        listOf(
            GastonomoRoute.Home,
            GastonomoRoute.Movements,
            GastonomoRoute.Loans,
            GastonomoRoute.Settings,
        ).forEach { route ->
            assertEquals(ScreenKind.TAB, screenKindOf(destinationOf(route::class.qualifiedName)))
        }
    }

    @Test
    fun `the two creation forms are forms, arguments and all`() {
        val newMovement = GastonomoRoute.NewMovement::class.qualifiedName
        assertEquals(ScreenKind.FORM, screenKindOf(destinationOf("$newMovement/{kind}")))
        assertEquals(
            ScreenKind.FORM,
            screenKindOf(destinationOf(GastonomoRoute.NewLoan::class.qualifiedName)),
        )
    }

    @Test
    fun `everything else is a stacked screen`() {
        val detail = GastonomoRoute.LoanDetail::class.qualifiedName
        assertEquals(ScreenKind.PUSHED, screenKindOf(destinationOf("$detail/{loanId}")))
        assertEquals(ScreenKind.PUSHED, screenKindOf(destinationOf(null)))
        assertEquals(ScreenKind.PUSHED, screenKindOf(null))
    }

    @Test
    fun `a form opens and closes with the animations wired`() {
        compose.setContent { GastonomoApp(container(), GastonomoRoute.Movements) }

        compose.onNodeWithText("Nuevo").performClick()
        compose.onNodeWithText("Nuevo movimiento").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cerrar").performClick()

        compose.onNodeWithText("Nuevo").assertIsDisplayed()
    }

    @Test
    fun `changing tab still lands with the animations wired`() {
        compose.setContent { GastonomoApp(container(), GastonomoRoute.Home) }

        compose.onNodeWithText("Ajustes").performClick()

        compose.onNodeWithText("Sincronización en la nube").assertIsDisplayed()
    }

    private fun container() = AppContainer(ApplicationProvider.getApplicationContext<Context>())

    private fun destinationOf(route: String?): NavDestination =
        NavDestination(NAVIGATOR).also { it.route = route }

    private companion object {
        const val NAVIGATOR = "composable"
    }
}
