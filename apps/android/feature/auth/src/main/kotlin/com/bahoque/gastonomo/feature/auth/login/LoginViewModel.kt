package com.bahoque.gastonomo.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.AuthValidation
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.toAuthMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Login. Valida antes de llamar para no gastar el limite de peticiones de la API con un
 * correo mal escrito, y reparte el 422 campo por campo con el mensaje que manda la API,
 * que ya viene en castellano.
 */
class LoginViewModel(private val signIn: SignIn) : ViewModel() {

    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) = mutableState.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) = mutableState.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun onTogglePasswordVisibility() = mutableState.update {
        it.copy(passwordVisible = !it.passwordVisible)
    }

    fun onSignedInHandled() = mutableState.update { it.copy(signedIn = false) }

    fun onSubmit() {
        val current = mutableState.value
        if (current.submitting) return
        val invalid = current.withLocalErrors()
        if (invalid != null) {
            mutableState.value = invalid
            return
        }
        mutableState.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = signIn(current.email, current.password)) {
                is Outcome.Success -> mutableState.update { it.copy(submitting = false, signedIn = true) }
                is Outcome.Failure -> mutableState.update { it.withFailure(result.error) }
            }
        }
    }

    /** El estado con los errores de lo que falta, o null si el formulario esta completo. */
    private fun LoginUiState.withLocalErrors(): LoginUiState? {
        val emailError = when {
            email.isBlank() -> AuthMessage.Resource(R.string.auth_error_email_required)
            !AuthValidation.isValidEmail(email) -> AuthMessage.Resource(R.string.auth_error_email_invalid)
            else -> null
        }
        val passwordError = if (password.isEmpty()) {
            AuthMessage.Resource(R.string.auth_error_password_required)
        } else {
            null
        }
        if (emailError == null && passwordError == null) return null
        return copy(emailError = emailError, passwordError = passwordError)
    }

    private fun LoginUiState.withFailure(error: ApiError): LoginUiState = when (error) {
        is ApiError.Validation -> copy(
            submitting = false,
            emailError = error.messageFor(EMAIL_FIELD)?.let(AuthMessage::Literal),
            passwordError = error.messageFor(PASSWORD_FIELD)?.let(AuthMessage::Literal),
            formError = error.takeIf { it.fields.isEmpty() }?.toAuthMessage(),
        )
        else -> copy(submitting = false, formError = error.toAuthMessage())
    }

    private companion object {
        /** Los nombres de campo que usa la API en `errors[].field`. */
        const val EMAIL_FIELD = "email"
        const val PASSWORD_FIELD = "password"
    }
}
