package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Comprueba que las tres rutas de esta feature montan su pantalla y que se llega de una a
 * otra. El sheet de abono no es una ruta: se abre encima sin navegar.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class LoansNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    private val dependencies = TestLoansDependencies()

    /** El aviso de prestamo creado. Ahora llega de fuera porque tambien lo levanta el dictado. */
    private val created = LoanCreatedSignal()

    @Test
    fun `the loans screen shows the loans of the account`() {
        compose.renderNavGraph(GastonomoRoute.Loans) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithText("Préstamos").assertIsDisplayed()
        compose.onNodeWithText("Collar Johana").assertIsDisplayed()
    }

    @Test
    fun `the quick add button navigates to the new loan form`() {
        val visited = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Loans, visited) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Nuevo préstamo").performClick()

        assertEquals(listOf<GastonomoRoute>(GastonomoRoute.NewLoan()), visited)
        compose.onNodeWithText("Nuevo préstamo").assertIsDisplayed()
    }

    // Dictar tambien sale de este boton, pero su pantalla la monta :app: probarlo aqui navegaria a
    // una ruta que este grafo no tiene. Esta cubierto en GastonomoTransitionsTest, con el grafo
    // entero.

    @Test
    fun `a card navigates to the detail of its loan`() {
        val visited = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Loans, visited) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithText("Collar Johana").performClick()

        assertEquals(
            listOf<GastonomoRoute>(GastonomoRoute.LoanDetail(SampleFinance.partialLoan.id.value)),
            visited,
        )
        compose.onNodeWithText("Detalle del préstamo").assertIsDisplayed()
    }

    @Test
    fun `the detail screen is reachable on its own`() {
        compose.renderNavGraph(GastonomoRoute.LoanDetail(SampleFinance.paidLoan.id.value)) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithText("Detalle del préstamo").assertIsDisplayed()
        compose.onNodeWithText("Historial de abonos").assertIsDisplayed()
    }

    @Test
    fun `the new loan screen is reachable on its own`() {
        compose.renderNavGraph(GastonomoRoute.NewLoan()) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithText("Nuevo préstamo").assertIsDisplayed()
        compose.onNodeWithText("Crear préstamo").assertIsDisplayed()
    }

    @Test
    fun `registering a payment opens the sheet without leaving the list`() {
        val visited = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Loans, visited) { actions ->
            loansNavGraph(dependencies, actions, created)
        }

        compose.onNodeWithText("Collar Johana").assertIsDisplayed()
        compose.onAllNodesWithText("Registrar abono").onFirst().performClick()

        assertEquals(emptyList<GastonomoRoute>(), visited)
        compose.onNodeWithText("Guardar abono").assertIsDisplayed()
    }
}
