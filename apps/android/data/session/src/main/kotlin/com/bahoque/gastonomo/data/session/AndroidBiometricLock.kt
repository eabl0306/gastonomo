package com.bahoque.gastonomo.data.session

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.bahoque.gastonomo.core.application.port.BiometricAvailability
import com.bahoque.gastonomo.core.application.port.BiometricLock
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * La huella del dispositivo con androidx.biometric.
 *
 * Necesita una [FragmentActivity] viva, asi que no se construye una vez en el contenedor:
 * el contenedor guarda un proveedor y la actividad actual se registra al crearse. Si no
 * hay actividad, la peticion se responde como cancelada en vez de fallar.
 */
class AndroidBiometricLock(
    private val context: android.content.Context,
    private val activityProvider: () -> FragmentActivity?,
) : BiometricLock {

    override suspend fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }

    override suspend fun authenticate(text: BiometricPromptText): BiometricResult {
        val activity = activityProvider() ?: return BiometricResult.Cancelled
        return withContext(Dispatchers.Main) { prompt(activity, text) }
    }

    private suspend fun prompt(
        activity: FragmentActivity,
        text: BiometricPromptText,
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(
            activity,
            androidx.core.content.ContextCompat.getMainExecutor(activity),
            Callback { continuation.resume(it) },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(text.title)
                .setSubtitle(text.subtitle)
                .setNegativeButtonText(text.negativeButton)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }

    /** Traduce las tres salidas de BiometricPrompt a [BiometricResult]. Solo responde una vez. */
    private class Callback(private val onResult: (BiometricResult) -> Unit) :
        BiometricPrompt.AuthenticationCallback() {

        private var answered = false

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            answerOnce(BiometricResult.Succeeded)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_CANCELED
            answerOnce(
                if (cancelled) BiometricResult.Cancelled else BiometricResult.Failed(errString.toString()),
            )
        }

        // onAuthenticationFailed se dispara por cada huella que no cuadra y el dialogo sigue
        // abierto, asi que no termina la espera: la termina onAuthenticationError.

        private fun answerOnce(result: BiometricResult) {
            if (answered) return
            answered = true
            onResult(result)
        }
    }

    private companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
