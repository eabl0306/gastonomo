package com.bahoque.gastonomo.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
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
 * Registro. El prototipo no valida nada; aqui si, porque el handoff lo pide y porque la
 * API responde 422 con las mismas tres reglas: correo valido, contrasena de ocho o mas y
 * terminos aceptados. El medidor de fuerza no bloquea nada.
 */
class RegisterViewModel(private val signUp: SignUp) : ViewModel() {

    private val mutableState = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = mutableState.asStateFlow()

    fun onNameChange(value: String) = mutableState.update {
        it.copy(name = value, nameError = null, formError = null)
    }

    fun onEmailChange(value: String) = mutableState.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) = mutableState.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun onToggleTerms() = mutableState.update {
        it.copy(acceptedTerms = !it.acceptedTerms, termsError = null, formError = null)
    }

    fun onRegisteredHandled() = mutableState.update { it.copy(registered = false, submitting = false) }

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
            val request = RegistrationRequest(
                name = current.name,
                email = current.email,
                password = current.password,
                acceptedTerms = current.acceptedTerms,
            )
            when (val result = signUp(request)) {
                // `submitting` sigue en true: la cuenta ya existe y el formulario
                // queda inerte mientras se ensena el toast y se navega a Inicio.
                is Outcome.Success -> mutableState.update { it.copy(registered = true) }
                is Outcome.Failure -> mutableState.update { it.withFailure(result.error) }
            }
        }
    }

    private fun RegisterUiState.withFailure(error: ApiError): RegisterUiState = when (error) {
        is ApiError.Validation -> copy(
            submitting = false,
            nameError = error.messageFor(NAME_FIELD)?.let(AuthMessage::Literal),
            emailError = error.messageFor(EMAIL_FIELD)?.let(AuthMessage::Literal),
            passwordError = error.messageFor(PASSWORD_FIELD)?.let(AuthMessage::Literal),
            termsError = error.messageFor(TERMS_FIELD)?.let(AuthMessage::Literal),
            formError = error.takeIf { it.fields.isEmpty() }?.toAuthMessage(),
        )
        // El 409 de correo ya registrado llega con su `detail` en castellano: se pinta
        // junto al campo de correo, que es el que hay que cambiar.
        is ApiError.Conflict -> copy(submitting = false, emailError = error.toAuthMessage())
        else -> copy(submitting = false, formError = error.toAuthMessage())
    }

    private companion object {
        const val NAME_FIELD = "name"
        const val EMAIL_FIELD = "email"
        const val PASSWORD_FIELD = "password"
        const val TERMS_FIELD = "acceptedTerms"
    }
}

/** El estado con lo que falta marcado, o null si se puede enviar. */
private fun RegisterUiState.withLocalErrors(): RegisterUiState? {
    val errors = copy(
        nameError = nameProblem(),
        emailError = emailProblem(),
        passwordError = passwordProblem(),
        termsError = termsProblem(),
    )
    val complete = errors.nameError == null && errors.emailError == null &&
        errors.passwordError == null && errors.termsError == null
    return errors.takeIf { !complete }
}

private fun RegisterUiState.nameProblem(): AuthMessage? =
    if (name.isBlank()) AuthMessage.Resource(R.string.auth_error_name_required) else null

private fun RegisterUiState.termsProblem(): AuthMessage? =
    if (acceptedTerms) null else AuthMessage.Resource(R.string.auth_error_terms_required)

private fun RegisterUiState.emailProblem(): AuthMessage? = when {
    email.isBlank() -> AuthMessage.Resource(R.string.auth_error_email_required)
    !AuthValidation.isValidEmail(email) -> AuthMessage.Resource(R.string.auth_error_email_invalid)
    else -> null
}

private fun RegisterUiState.passwordProblem(): AuthMessage? = when {
    password.isEmpty() -> AuthMessage.Resource(R.string.auth_error_password_required)
    !AuthValidation.isLongEnough(password) ->
        AuthMessage.Resource(R.string.auth_error_password_too_short)
    else -> null
}
