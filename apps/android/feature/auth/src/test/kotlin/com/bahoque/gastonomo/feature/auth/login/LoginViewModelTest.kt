package com.bahoque.gastonomo.feature.auth.login

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.MainDispatcherRule
import com.bahoque.gastonomo.feature.auth.R
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `an empty form is not sent and marks both fields`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthMessage.Resource(R.string.auth_error_email_required), state.emailError)
        assertEquals(AuthMessage.Resource(R.string.auth_error_password_required), state.passwordError)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a malformed email is marked without calling the api`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onEmailChange("camilo.correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Resource(R.string.auth_error_email_invalid),
            viewModel.state.value.emailError,
        )
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a good login signs in and hands the credentials over trimmed`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onEmailChange("  camilo@correo.com ")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.signedIn)
        assertFalse(viewModel.state.value.submitting)
        assertEquals("camilo@correo.com" to "secreta1", gateway.lastCredentials)
    }

    @Test
    fun `the signed in flag is consumed so a recomposition does not navigate twice`() = runTest {
        val viewModel = viewModelWith(FakeAuthGateway())

        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onSignedInHandled()

        assertFalse(viewModel.state.value.signedIn)
    }

    @Test
    fun `wrong credentials become a form error and not a field error`() = runTest {
        val gateway = FakeAuthGateway(sessionResult = Outcome.Failure(ApiError.InvalidCredentials))
        val viewModel = viewModelWith(gateway)

        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthMessage.Resource(R.string.auth_error_invalid_credentials), state.formError)
        assertNull(state.emailError)
        assertFalse(state.signedIn)
    }

    @Test
    fun `a 422 lands on the field the api names, with its own message`() = runTest {
        val validation = ApiError.Validation(
            fields = listOf(FieldError(field = "email", message = "El correo no es valido.")),
        )
        val viewModel = viewModelWith(FakeAuthGateway(sessionResult = Outcome.Failure(validation)))

        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Literal("El correo no es valido."),
            viewModel.state.value.emailError,
        )
        assertNull(viewModel.state.value.formError)
    }

    @Test
    fun `no connection shows the shared network message`() = runTest {
        val viewModel = viewModelWith(FakeAuthGateway(sessionResult = Outcome.Failure(ApiError.Network)))

        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.formError is AuthMessage.Resource)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `typing again clears the error of that field`() = runTest {
        val viewModel = viewModelWith(FakeAuthGateway())

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onEmailChange("c")

        assertNull(viewModel.state.value.emailError)
        assertEquals(
            AuthMessage.Resource(R.string.auth_error_password_required),
            viewModel.state.value.passwordError,
        )
    }

    @Test
    fun `the eye toggles the visibility of the password`() {
        val viewModel = viewModelWith(FakeAuthGateway())

        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.state.value.passwordVisible)
        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.state.value.passwordVisible)
    }

    @Test
    fun `a second press while it is sending does not call the api twice`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange("secreta1")
        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("login"), gateway.calls)
    }

    private fun viewModelWith(gateway: FakeAuthGateway) =
        LoginViewModel(SignIn(gateway, FakeSessionStore()))
}
