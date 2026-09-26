package com.bahoque.gastonomo.core.application.port

/**
 * El cerrojo local: PIN de cuatro digitos y huella. Protege el refresh token guardado en
 * el dispositivo y **no toca la API**, asi que no se sincroniza entre telefonos.
 *
 * El PIN se guarda hasheado, nunca en claro.
 */
interface LocalLock {

    /** True si hay un PIN configurado en este dispositivo. */
    suspend fun hasPin(): Boolean

    /** Guarda el PIN. Se le pasan los cuatro digitos tal cual; el adaptador los hashea. */
    suspend fun setPin(pin: String)

    /** Comprueba el PIN teclado contra el guardado. */
    suspend fun verifyPin(pin: String): Boolean

    /** Quita el PIN. Se llama al apagar el toggle de Ajustes y al cerrar sesion. */
    suspend fun clearPin()

    companion object {
        /** El teclado del diseno tiene cuatro puntos. */
        const val PIN_LENGTH = 4
    }
}
