package com.bahoque.gastonomo.feature.auth.login

import com.bahoque.gastonomo.feature.auth.AuthMessage

/**
 * Lo que pinta la pantalla de Login, como un unico valor inmutable.
 *
 * @property formError el fallo que no es de un campo concreto: credenciales, red o limite
 *   de peticiones. Va encima del boton.
 * @property signedIn true cuando el login salio bien. Lo consume la ruta para navegar a
 *   Inicio y se apaga con `onSignedInHandled`, para que un cambio de configuracion no
 *   vuelva a navegar.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    val emailError: AuthMessage? = null,
    val passwordError: AuthMessage? = null,
    val formError: AuthMessage? = null,
    val signedIn: Boolean = false,
)
