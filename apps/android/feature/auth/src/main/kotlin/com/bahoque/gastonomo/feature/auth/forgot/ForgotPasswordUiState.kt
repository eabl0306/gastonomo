package com.bahoque.gastonomo.feature.auth.forgot

import com.bahoque.gastonomo.feature.auth.AuthMessage

/**
 * Los dos estados de Recuperar contrasena: el formulario y el "Revisa tu correo".
 *
 * La API responde **202 exista o no la cuenta**, asi que se pasa a [sent] siempre que la
 * llamada no falle por red: responder distinto delataria quien tiene cuenta.
 *
 * @property sentTo el correo que se ensena en el segundo estado. Se congela al enviar, para
 *   que seguir escribiendo no cambie la frase.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val sent: Boolean = false,
    val sentTo: String = "",
    val submitting: Boolean = false,
    val emailError: AuthMessage? = null,
    val formError: AuthMessage? = null,
    val resentToast: Boolean = false,
)
