package com.bahoque.gastonomo.feature.auth.register

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `the prototype does not validate but this does the three rules of the handoff`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthMessage.Resource(R.string.auth_error_name_required), state.nameError)
        assertEquals(AuthMessage.Resource(R.string.auth_error_email_required), state.emailError)
        assertEquals(AuthMessage.Resource(R.string.auth_error_password_required), state.passwordError)
        assertEquals(AuthMessage.Resource(R.string.auth_error_terms_required), state.termsError)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a short password is marked and the api is not called`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        fillForm(viewModel, password = "corta")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Resource(R.string.auth_error_password_too_short),
            viewModel.state.value.passwordError,
        )
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `without the terms it does not register`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        fillForm(viewModel, acceptTerms = false)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.termsError)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `a complete form registers and trims the name and the email`() = runTest {
        val gateway = FakeAuthGateway()
        val viewModel = viewModelWith(gateway)

        viewModel.onNameChange("  Camilo Rojas ")
        viewModel.onEmailChange(" camilo@correo.com ")
        viewModel.onPasswordChange("secreta1")
        viewModel.onToggleTerms()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.registered)
        val request = assertNotNull(gateway.lastRegistration)
        assertEquals("Camilo Rojas", request.name)
        assertEquals("camilo@correo.com", request.email)
        assertTrue(request.acceptedTerms)
    }

    @Test
    fun `the form stays inert while the toast is showing`() = runTest {
        val viewModel = viewModelWith(FakeAuthGateway())

        fillForm(viewModel)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.submitting)
        viewModel.onRegisteredHandled()
        assertTrue(!viewModel.state.value.submitting && !viewModel.state.value.registered)
    }

    @Test
    fun `an already registered email lands on the email field with the message of the api`() = runTest {
        val conflict = ApiError.Conflict(
            code = "email_taken",
            detail = "Ya existe una cuenta con ese correo.",
        )
        val viewModel = viewModelWith(FakeAuthGateway(sessionResult = Outcome.Failure(conflict)))

        fillForm(viewModel)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Literal("Ya existe una cuenta con ese correo."),
            viewModel.state.value.emailError,
        )
        assertNull(viewModel.state.value.formError)
    }

    @Test
    fun `a 422 is spread field by field with the messages of the api`() = runTest {
        val validation = ApiError.Validation(
            fields = listOf(
                FieldError("password", "La contrasena necesita 8 caracteres."),
                FieldError("acceptedTerms", "Tienes que aceptar los terminos."),
            ),
        )
        val viewModel = viewModelWith(FakeAuthGateway(sessionResult = Outcome.Failure(validation)))

        fillForm(viewModel)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Literal("La contrasena necesita 8 caracteres."),
            viewModel.state.value.passwordError,
        )
        assertEquals(
            AuthMessage.Literal("Tienes que aceptar los terminos."),
            viewModel.state.value.termsError,
        )
    }

    @Test
    fun `the rate limit of the api becomes a form error`() = runTest {
        val tooMany = ApiError.TooManyRequests(retryAfterSeconds = 30)
        val viewModel = viewModelWith(FakeAuthGateway(sessionResult = Outcome.Failure(tooMany)))

        fillForm(viewModel)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(
            AuthMessage.Resource(R.string.auth_error_too_many_requests),
            viewModel.state.value.formError,
        )
    }

    @Test
    fun `the strength meter is derived from what is typed`() {
        val viewModel = viewModelWith(FakeAuthGateway())

        assertEquals(PasswordStrength.EMPTY, viewModel.state.value.strength)
        viewModel.onPasswordChange("abc")
        assertEquals(PasswordStrength.VERY_SHORT, viewModel.state.value.strength)
        viewModel.onPasswordChange("secreta1")
        assertEquals(PasswordStrength.STRONG, viewModel.state.value.strength)
    }

    private fun fillForm(
        viewModel: RegisterViewModel,
        password: String = "secreta1",
        acceptTerms: Boolean = true,
    ) {
        viewModel.onNameChange("Camilo Rojas")
        viewModel.onEmailChange("camilo@correo.com")
        viewModel.onPasswordChange(password)
        if (acceptTerms) viewModel.onToggleTerms()
    }

    private fun viewModelWith(gateway: FakeAuthGateway) =
        RegisterViewModel(SignUp(gateway, FakeSessionStore()))
}
