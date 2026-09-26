package com.bahoque.gastonomo.feature.loans

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailActions
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailScreen
import com.bahoque.gastonomo.feature.loans.form.NewLoanActions
import com.bahoque.gastonomo.feature.loans.form.NewLoanScreen
import com.bahoque.gastonomo.feature.loans.list.LoansActions
import com.bahoque.gastonomo.feature.loans.list.LoansScreen
import com.bahoque.gastonomo.feature.loans.payment.PaymentActions
import com.bahoque.gastonomo.feature.loans.payment.RegisterPaymentSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Las capturas de las pantallas 8 a 11, para auditar la fidelidad contra el prototipo sin
 * emulador. Salen en `feature/loans/build/outputs/roborazzi`.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = SPANISH_QUALIFIERS)
class LoansScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `08 loans`() {
        compose.captureGastonomoScreen("08-loans") {
            LoansScreen(state = LoansSamples.loans, actions = noopLoansActions())
        }
    }

    @Test
    fun `08 loans empty`() {
        compose.captureGastonomoScreen("08-loans-empty") {
            LoansScreen(state = LoansSamples.emptyLoans, actions = noopLoansActions())
        }
    }

    @Test
    fun `09 loan detail`() {
        compose.captureGastonomoScreen("09-loan-detail") {
            LoanDetailScreen(state = LoansSamples.detail, actions = noopDetailActions())
        }
    }

    @Test
    fun `09 loan detail paid with interest`() {
        compose.captureGastonomoScreen("09-loan-detail-paid") {
            LoanDetailScreen(state = LoansSamples.paidDetail, actions = noopDetailActions())
        }
    }

    @Test
    fun `10 new loan`() {
        compose.captureGastonomoScreen("10-new-loan") {
            NewLoanScreen(state = LoansSamples.newLoan, actions = noopFormActions())
        }
    }

    @Test
    fun `10 new loan in another currency`() {
        compose.captureGastonomoScreen("10-new-loan-usd") {
            NewLoanScreen(state = LoansSamples.newLoanInDollars, actions = noopFormActions())
        }
    }

    @Test
    fun `11 register payment`() {
        compose.captureGastonomoScreen("11-register-payment") {
            RegisterPaymentSheet(state = LoansSamples.payment, actions = noopPaymentActions())
        }
    }

    private fun noopLoansActions() = LoansActions({}, {}, {}, {}, {})

    private fun noopDetailActions() = LoanDetailActions({}, {}, {})

    private fun noopFormActions() =
        NewLoanActions(moneyEntryActions { }, {}, {}, {}, {}, {}, {})

    private fun noopPaymentActions() =
        PaymentActions(moneyEntryActions { }, {}, {}, {}, {})
}
