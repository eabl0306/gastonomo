package com.bahoque.gastonomo.feature.auth.unlock

import com.bahoque.gastonomo.feature.auth.AuthMessage

/**
 * Lo que pinta la pantalla de desbloqueo.
 *
 * @property digits cuantos de los cuatro puntos estan rellenos. No se guarda el PIN
 *   escrito en el estado de UI a proposito: lo tiene el ViewModel y sale de ahi solo para
 *   comprobarlo.
 * @property attemptsLeft intentos que quedan antes de borrar la sesion del dispositivo.
 * @property unlocked true cuando se entro, con PIN o con huella.
 * @property lockedOut true cuando se agotaron los intentos: se borro la sesion local y hay
 *   que entrar con la contrasena.
 */
data class UnlockUiState(
    val name: String = "",
    val initials: String = "",
    val digits: Int = 0,
    val attemptsLeft: Int = MAXIMUM_ATTEMPTS,
    val checking: Boolean = false,
    val message: AuthMessage? = null,
    val unlocked: Boolean = false,
    val lockedOut: Boolean = false,
) {
    /**
     * Con los intentos agotados el teclado deja de responder: la unica salida es el enlace
     * "Usar contrasena", que ya esta en la pantalla.
     */
    val acceptsInput: Boolean get() = !checking && !lockedOut

    companion object {
        /** Los cuatro puntos del diseno. */
        const val PIN_LENGTH = 4

        /**
         * Intentos antes de borrar la sesion guardada. El diseno no lo dice; cuatro digitos
         * son diez mil combinaciones y sin limite el cerrojo no protege nada.
         */
        const val MAXIMUM_ATTEMPTS = 5
    }
}
