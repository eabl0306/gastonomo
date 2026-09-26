package com.bahoque.gastonomo.feature.auth.forgot

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.MainDispatcherRule
import com.bahoque.gastonomo.feature.auth.R
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForgotPasswordViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `the email comes prefilled with the one typed in login`() {
        val viewModel = viewModelWith(FakeAuthGateway(), initialEmail = "camilo@correo.com")

        assertEquals("camilo@correo.com", viewModel.state.value.email)
    }

    @Test
    fun `sending it moves to check your mail and freezes the address`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway, initialEmail = "camilo@correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onEmailChange("otro@correo.com")

        assertTrue(viewModel.state.value.sent)
        assertEquals("camilo@correo.com", viewModel.state.value.sentTo)
        assertEquals(listOf("forgot:camilo@correo.com"), gateway.calls)
    }

    @Test
    fun `an account that does not exist looks exactly the same`() = runTest {
        // La API responde 202 exista el correo o no, asi que la pantalla no puede delatarlo.
        val gateway = FakeAuthGateway(unitResult = Outcome.Success(Unit))
        val viewModel = viewModelWith(gateway, initialEmail = "nadie@correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.resentToast)
    }

    @Test
    fun `a malformed email is marked without calling the api`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway, initialEmail = "camilo.correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Resource(R.string.auth_error_email_invalid),
            viewModel.state.value.emailError,
        )
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `resending uses the same endpoint and raises the toast`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway, initialEmail = "camilo@correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onResend()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.resentToast)
        assertEquals(2, gateway.calls.size)
        viewModel.onToastHidden()
        assertFalse(viewModel.state.value.resentToast)
    }

    @Test
    fun `without connection it stays on the form with the shared message`() = runTest {
        val gateway = FakeAuthGateway(unitResult = Outcome.Failure(ApiError.Network))
        val viewModel = viewModelWith(gateway, initialEmail = "camilo@correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.sent)
        assertTrue(viewModel.state.value.formError is AuthMessage.Resource)
    }

    @Test
    fun `the rate limit of resending becomes a form error`() = runTest {
        val gateway = FakeAuthGateway(unitResult = Outcome.Failure(ApiError.TooManyRequests()))
        val viewModel = viewModelWith(gateway, initialEmail = "camilo@correo.com")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Resource(R.string.auth_error_too_many_requests),
            viewModel.state.value.formError,
        )
    }

    private fun viewModelWith(gateway: FakeAuthGateway, initialEmail: String = "") =
        ForgotPasswordViewModel(RequestPasswordReset(gateway), initialEmail)
}
