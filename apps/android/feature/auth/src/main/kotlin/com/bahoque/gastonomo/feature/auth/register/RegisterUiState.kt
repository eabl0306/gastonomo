package com.bahoque.gastonomo.feature.auth.register

import com.bahoque.gastonomo.feature.auth.AuthMessage

/**
 * Lo que pinta la pantalla de Registro.
 *
 * @property registered true cuando la cuenta se creo. Lo consume la ruta para ir a Inicio
 *   con el toast "Cuenta creada", y se apaga con `onRegisteredHandled`.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    val submitting: Boolean = false,
    val nameError: AuthMessage? = null,
    val emailError: AuthMessage? = null,
    val passwordError: AuthMessage? = null,
    val termsError: AuthMessage? = null,
    val formError: AuthMessage? = null,
    val registered: Boolean = false,
) {
    /** El medidor de fuerza no se guarda: se deriva de lo escrito. */
    val strength: PasswordStrength get() = PasswordStrength.of(password)
}
