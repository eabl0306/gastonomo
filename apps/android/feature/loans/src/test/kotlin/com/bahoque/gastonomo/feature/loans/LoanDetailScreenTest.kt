package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailActions
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailScreen
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class LoanDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows the balance, the three columns and the note`() {
        render(LoansSamples.detail)

        compose.onNodeWithText("Detalle del préstamo").assertIsDisplayed()
        compose.onNodeWithText("Saldo pendiente").assertIsDisplayed()
        compose.onNodeWithText("Original").assertIsDisplayed()
        compose.onNodeWithText("Abonado").assertIsDisplayed()
        compose.onNodeWithText("Parcial · 67%").assertIsDisplayed()
        compose.onNodeWithText("Collar").assertIsDisplayed()
        compose.onNodeWithText("Creado el 2 jul 2026").assertIsDisplayed()
    }

    @Test
    fun `an open loan offers to register a payment`() {
        val payments = mutableListOf<String>()
        render(LoansSamples.detail, onRegisterPayment = { payments += it.loan.id.value })

        compose.onNodeWithText("Registrar abono").performClick()

        assertEquals(1, payments.size)
    }

    @Test
    fun `a paid loan shows the interest and no payment button`() {
        render(LoansSamples.paidDetail)

        compose.onNodeWithText("Intereses ganados").assertIsDisplayed()
        compose.onAllNodesWithText("Registrar abono").assertCountEquals(0)
    }

    @Test
    fun `the history lists every payment with its concept`() {
        render(LoansSamples.paidDetail)

        compose.onNodeWithText("Historial de abonos").assertIsDisplayed()
        compose.onNodeWithText("Abono · Intereses").assertIsDisplayed()
        compose.onNodeWithText("Abono").assertIsDisplayed()
    }

    @Test
    fun `a loan without payments says so`() {
        val pending = LoansSamples.loans.rows.first { it.loan.payments.isEmpty() }
        render(LoanDetailUiState(loading = false, row = pending, context = LoansSamples.context))

        compose.onNodeWithText("Aún no hay abonos.").assertIsDisplayed()
    }

    @Test
    fun `a failure offers to retry`() {
        var retries = 0
        val state = LoanDetailUiState(loading = false, problem = LoanProblem.NOT_FOUND)
        render(state, onRetry = { retries++ })

        compose.onNodeWithText("No encontramos este préstamo.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    private fun render(
        state: LoanDetailUiState,
        onRegisterPayment: (LoanRow) -> Unit = {},
        onRetry: () -> Unit = {},
    ) = compose.setContent {
        GastonomoTheme {
            LoanDetailScreen(
                state = state,
                actions = LoanDetailActions(
                    onBack = {},
                    onRegisterPayment = onRegisterPayment,
                    onRetry = onRetry,
                ),
            )
        }
    }
}
