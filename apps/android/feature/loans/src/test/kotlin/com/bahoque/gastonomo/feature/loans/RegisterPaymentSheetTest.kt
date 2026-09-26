package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.payment.PaymentActions
import com.bahoque.gastonomo.feature.loans.payment.PaymentPortion
import com.bahoque.gastonomo.feature.loans.payment.PaymentUiState
import com.bahoque.gastonomo.feature.loans.payment.RegisterPaymentSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class RegisterPaymentSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it names the person and its balance`() {
        render(LoansSamples.payment)

        compose.onNodeWithText("Registrar abono").assertIsDisplayed()
        compose.onNodeWithText("Collar Johana · saldo $ 50.000").assertIsDisplayed()
    }

    @Test
    fun `the chips fill the amount`() {
        val filled = mutableListOf<PaymentPortion>()
        render(LoansSamples.payment, onFill = { filled += it })

        compose.onNodeWithText("Todo el saldo").performClick()
        compose.onNodeWithText("La mitad").performClick()

        assertEquals(listOf(PaymentPortion.ALL, PaymentPortion.HALF), filled)
    }

    @Test
    fun `saving is offered and reported`() {
        var saves = 0
        render(LoansSamples.payment, onSave = { saves++ })

        compose.onNodeWithText("Guardar abono").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `a payment in another currency shows the conversion box`() {
        val state = LoansSamples.payment.copy(
            entry = MoneyEntryState(
                amountRaw = "16",
                currency = com.bahoque.gastonomo.core.domain.money.CurrencyCode.USD,
            ),
        )
        render(state)

        compose.onNodeWithText("Equivale a (COP)").assertIsDisplayed()
    }

    @Test
    fun `a failure is shown without closing the sheet`() {
        val state = LoansSamples.payment.copy(
            failure = LoanSaveFailure(problem = LoanProblem.IN_PROGRESS),
        )
        render(state)

        compose.onNodeWithText(
            "Ese guardado ya está en curso. Espera un momento y vuelve a intentarlo.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Guardar abono").assertIsDisplayed()
    }

    private fun render(
        state: PaymentUiState,
        onFill: (PaymentPortion) -> Unit = {},
        onSave: () -> Unit = {},
    ) = compose.setContent {
        GastonomoTheme {
            RegisterPaymentSheet(
                state = state,
                actions = PaymentActions(
                    entry = moneyEntryActions { },
                    onFill = onFill,
                    onDate = {},
                    onSave = onSave,
                    onDismiss = {},
                ),
            )
        }
    }
}
