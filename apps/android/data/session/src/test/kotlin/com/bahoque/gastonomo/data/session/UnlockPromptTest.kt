package com.bahoque.gastonomo.data.session

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Construir el dialogo del sistema no necesita sensor de huella ni actividad, asi que si se
 * puede ejecutar en la JVM, y hace falta.
 *
 * `PromptInfo.Builder.build()` valida la combinacion de textos y autenticadores, y la que
 * habia era invalida: la aplicacion se cerraba con `IllegalArgumentException` al tocar la
 * huella, en produccion. El test que ya existia del cerrojo no llegaba aqui, porque prueba el
 * camino de "no hay actividad viva", que vuelve antes de construir nada.
 */
@RunWith(AndroidJUnit4::class)
class UnlockPromptTest {

    private val text = BiometricPromptText(
        title = "Desbloquea Gastonomo",
        subtitle = "Usa tu huella para entrar.",
        negativeButton = "Usar PIN",
    )

    @Test
    fun `it builds the system dialog with the texts the screen resolved`() {
        val info = unlockPromptInfo(text)

        assertEquals("Desbloquea Gastonomo", info.title.toString())
        assertEquals("Usa tu huella para entrar.", info.subtitle?.toString())
        assertEquals("Usar PIN", info.negativeButtonText.toString())
    }

    @Test
    fun `it does not allow the device credential, because the PIN button is ours`() {
        val allowed = unlockPromptInfo(text).allowedAuthenticators

        assertFalse(
            allowed and BiometricManager.Authenticators.DEVICE_CREDENTIAL != 0,
            "el boton dice 'Usar PIN' y ese es el PIN de cuatro digitos de la aplicacion; con " +
                "la credencial del dispositivo permitida se entraria con el patron del movil, " +
                "saltandose el cerrojo y su limite de intentos",
        )
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_WEAK, allowed)
    }

    /**
     * Pinta la regla de la libreria que provoco el cierre, para que nadie vuelva a juntar las
     * dos cosas y para que una subida de androidx.biometric que la cambie se note aqui.
     */
    @Test
    fun `asking for the device credential and a negative button at once is what crashed`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(text.title)
                .setNegativeButtonText(text.negativeButton)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build()
        }

        assertTrue(failure.message.orEmpty().contains("Negative text"), failure.message)
    }
}
