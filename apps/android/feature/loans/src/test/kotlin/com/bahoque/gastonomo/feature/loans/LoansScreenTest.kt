package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.list.LoansActions
import com.bahoque.gastonomo.feature.loans.list.LoansScreen
import com.bahoque.gastonomo.feature.loans.list.LoansUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class LoansScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows both totals with their pending balance`() {
        render(LoansSamples.loans)

        compose.onNodeWithText("Te deben").assertIsDisplayed()
        compose.onNodeWithText("Debes").assertIsDisplayed()
        // El unico abierto de cada lado: 50.000 que me deben y 600.000 que debo. Los mismos
        // importes salen tambien en su tarjeta, asi que hay dos nodos de cada uno.
        compose.onAllNodesWithText("$ 50.000").assertCountEquals(2)
        compose.onAllNodesWithText("$ 600.000").assertCountEquals(2)
        compose.onAllNodesWithText("1 préstamo").assertCountEquals(2)
    }

    @Test
    fun `an open loan offers to register a payment and a paid one does not`() {
        render(LoansSamples.loans)

        // Dos abiertos, asi que dos botones de abono; el pagado no lo tiene.
        compose.onAllNodesWithText("Registrar abono").assertCountEquals(2)
    }

    @Test
    fun `a paid loan with interest says so instead of showing a balance`() {
        render(LoansSamples.loans)

        compose.onNodeWithText("Intereses ganados").assertIsDisplayed()
    }

    @Test
    fun `the pills ask for the filter`() {
        val chosen = mutableListOf<LoanDirection?>()
        render(LoansSamples.loans, LoansActions(onFilter = { chosen += it }, {}, {}, {}, {}))

        compose.onNodeWithText("Debo").performClick()

        assertEquals(listOf<LoanDirection?>(LoanDirection.BORROWED), chosen)
    }

    @Test
    fun `an empty list explains it`() {
        render(LoansSamples.emptyLoans)

        compose.onNodeWithText("No tienes préstamos en esta lista.").assertIsDisplayed()
    }

    @Test
    fun `a failure offers to retry`() {
        var retries = 0
        render(LoansSamples.loansProblem, LoansActions({}, {}, {}, {}, onRetry = { retries++ }))

        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `the card opens the detail and the payment button does not`() {
        val opened = mutableListOf<String>()
        val payments = mutableListOf<String>()
        render(
            LoansSamples.loans,
            LoansActions(
                onFilter = {},
                onOpenLoan = { opened += it.loan.id.value },
                onRegisterPayment = { payments += it.loan.id.value },
                onNewLoan = {},
                onRetry = {},
            ),
        )

        compose.onAllNodesWithText("Registrar abono").onFirst().performClick()

        assertEquals(emptyList<String>(), opened)
        assertEquals(1, payments.size)
    }

    private fun render(
        state: LoansUiState,
        actions: LoansActions = LoansActions({}, {}, {}, {}, {}),
    ) = compose.setContent {
        GastonomoTheme { LoansScreen(state = state, actions = actions) }
    }
}
