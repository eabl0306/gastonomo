package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.form.NewLoanActions
import com.bahoque.gastonomo.feature.loans.form.NewLoanScreen
import com.bahoque.gastonomo.feature.loans.form.NewLoanUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class NewLoanScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `lending asks who you lent to and explains it will be an expense`() {
        render(LoansSamples.newLoan)

        compose.onNodeWithText("Nuevo préstamo").assertIsDisplayed()
        compose.onNodeWithText("¿A quién le prestaste?").assertIsDisplayed()
        compose.onNodeWithText(
            "Se registrará como gasto en tus movimientos. " +
                "Cada abono que recibas contará como ingreso.",
        ).assertIsDisplayed()
    }

    @Test
    fun `borrowing asks who lent to you and explains it will be income`() {
        render(LoansSamples.newLoan.copy(direction = LoanDirection.BORROWED))

        compose.onNodeWithText("¿Quién te prestó?").assertIsDisplayed()
        compose.onNodeWithText(
            "Se registrará como ingreso en tus movimientos. " +
                "Cada abono que pagues contará como gasto.",
        ).assertIsDisplayed()
    }

    @Test
    fun `the segmented control asks for the direction`() {
        val chosen = mutableListOf<LoanDirection>()
        render(LoansSamples.newLoan, onDirection = { chosen += it })

        compose.onNodeWithText("Me prestaron").performClick()

        assertEquals(listOf(LoanDirection.BORROWED), chosen)
    }

    @Test
    fun `an amount in the display currency hides the conversion box`() {
        render(LoansSamples.newLoan)

        compose.onAllNodesWithText("Tasa: 1 USD en COP").assertCountEquals(0)
        compose.onNodeWithText("Monto total").assertIsDisplayed()
    }

    @Test
    fun `a foreign amount shows the rate, and the equivalent worked out from it`() {
        render(LoansSamples.newLoanInDollars)

        compose.onNodeWithText("Tasa: 1 USD en COP").assertIsDisplayed()
        // Lo que se teclea es la tasa, y debajo se cuenta lo que sale: 140 por 3.199,41.
        compose.onNodeWithText("3.199,41").assertExists()
        compose.onNodeWithText(
            "Equivale a $ 447.917. Tasa de Frankfurter; puedes cambiarla.",
        ).assertExists()
    }

    @Test
    fun `an edited rate offers to go back to the suggested one`() {
        render(
            LoansSamples.newLoanInDollars.copy(
                entry = LoansSamples.newLoanInDollars.entry.withRate("4500"),
            ),
        )

        compose.onNodeWithText("Usar sugerida").assertIsDisplayed()
    }

    @Test
    fun `the button asks to create the loan`() {
        var saves = 0
        render(LoansSamples.newLoan, onSave = { saves++ })

        compose.onNodeWithText("Crear préstamo").performClick()

        assertEquals(1, saves)
    }

    private fun render(
        state: NewLoanUiState,
        onDirection: (LoanDirection) -> Unit = {},
        onSave: () -> Unit = {},
    ) = compose.setContent {
        GastonomoTheme {
            NewLoanScreen(
                state = state,
                actions = NewLoanActions(
                    entry = moneyEntryActions { },
                    onDirection = onDirection,
                    onPerson = {},
                    onNote = {},
                    onDate = {},
                    onSave = onSave,
                    onClose = {},
                ),
            )
        }
    }
}
