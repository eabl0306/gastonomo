package com.bahoque.gastonomo.feature.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
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
 * Recuperar contrasena.
 *
 * El correo llega prerellenado con el que se escribio en Login. El formato se comprueba en
 * local, pero el resultado de la llamada nunca dice si la cuenta existe.
 */
class ForgotPasswordViewModel(
    private val requestPasswordReset: RequestPasswordReset,
    initialEmail: String = "",
) : ViewModel() {

    private val mutableState = MutableStateFlow(ForgotPasswordUiState(email = initialEmail))
    val state: StateFlow<ForgotPasswordUiState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) = mutableState.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onToastHidden() = mutableState.update { it.copy(resentToast = false) }

    /** Enviar el enlace la primera vez: si sale bien, se pasa al estado "Revisa tu correo". */
    fun onSubmit() = send(resend = false)

    /** "No llego? Reenviar": el mismo endpoint, y el toast en vez de cambiar de estado. */
    fun onResend() = send(resend = true)

    private fun send(resend: Boolean) {
        val current = mutableState.value
        if (current.submitting) return
        val emailError = current.emailProblem()
        if (emailError != null) {
            mutableState.update { it.copy(emailError = emailError) }
            return
        }
        mutableState.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = requestPasswordReset(current.email)) {
                is Outcome.Success -> mutableState.update { it.withSent(resend) }
                is Outcome.Failure -> mutableState.update { it.withFailure(result.error) }
            }
        }
    }

    private fun ForgotPasswordUiState.emailProblem(): AuthMessage? = when {
        email.isBlank() -> AuthMessage.Resource(R.string.auth_error_email_required)
        !AuthValidation.isValidEmail(email) -> AuthMessage.Resource(R.string.auth_error_email_invalid)
        else -> null
    }

    private fun ForgotPasswordUiState.withSent(resend: Boolean): ForgotPasswordUiState = copy(
        submitting = false,
        sent = true,
        sentTo = email.trim(),
        resentToast = resend,
    )

    private fun ForgotPasswordUiState.withFailure(error: ApiError): ForgotPasswordUiState =
        when (error) {
            is ApiError.Validation -> copy(
                submitting = false,
                emailError = error.messageFor(EMAIL_FIELD)?.let(AuthMessage::Literal),
                formError = error.takeIf { it.fields.isEmpty() }?.toAuthMessage(),
            )
            else -> copy(submitting = false, formError = error.toAuthMessage())
        }

    private companion object {
        const val EMAIL_FIELD = "email"
    }
}
