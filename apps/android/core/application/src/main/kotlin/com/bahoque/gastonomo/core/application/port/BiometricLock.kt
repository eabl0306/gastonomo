package com.bahoque.gastonomo.core.application.port

/** Si el dispositivo puede pedir la huella. */
enum class BiometricAvailability {
    /** Hay hardware y hay una huella registrada. */
    AVAILABLE,

    /** Hay hardware pero el usuario no ha registrado ninguna huella. */
    NOT_ENROLLED,

    /** No hay hardware, o esta deshabilitado. */
    UNAVAILABLE,
}

/** Como acabo la peticion de huella. */
sealed interface BiometricResult {
    /** El usuario se autentico. */
    data object Succeeded : BiometricResult

    /** El usuario cancelo o pidio usar el PIN. */
    data object Cancelled : BiometricResult

    /** Fallo, con el mensaje que da el sistema. */
    data class Failed(val message: String?) : BiometricResult
}

/** Los textos del dialogo de huella, que salen de `strings.xml`. */
data class BiometricPromptText(
    val title: String,
    val subtitle: String,
    val negativeButton: String,
)

/**
 * La huella del dispositivo. Igual que el PIN, es un cerrojo local: desbloquea el refresh
 * token guardado y no habla con la API.
 */
interface BiometricLock {

    suspend fun availability(): BiometricAvailability

    /** Ensena el dialogo del sistema y espera. Suspende hasta que el usuario decide. */
    suspend fun authenticate(text: BiometricPromptText): BiometricResult
}
