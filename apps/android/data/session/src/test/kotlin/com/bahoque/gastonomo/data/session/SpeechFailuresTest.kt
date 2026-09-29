package com.bahoque.gastonomo.data.session

import android.os.Bundle
import android.speech.SpeechRecognizer
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Los codigos del reconocimiento traducidos al motivo que la pantalla sabe explicar.
 *
 * Esto se prueba aparte porque [AndroidSpeechToText] necesita un servicio de reconocimiento que la
 * JVM no tiene. Dejar el `when` dentro de esa clase era dejar sin probar lo unico que decide lo que
 * el usuario lee cuando el dictado falla, y ese error ya costo un cierre con la huella.
 */
@RunWith(RobolectricTestRunner::class)
class SpeechFailuresTest {

    @Test
    fun `no permission is its own reason, not a generic failure`() {
        assertEquals(
            SpeechFailure.PERMISSION,
            SpeechFailures.of(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
    }

    @Test
    fun `both network codes mean there is no connection`() {
        assertEquals(SpeechFailure.OFFLINE, SpeechFailures.of(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(
            SpeechFailure.OFFLINE,
            SpeechFailures.of(SpeechRecognizer.ERROR_NETWORK_TIMEOUT),
        )
    }

    @Test
    fun `silence and no match both mean we did not hear you`() {
        assertEquals(SpeechFailure.NO_MATCH, SpeechFailures.of(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(
            SpeechFailure.NO_MATCH,
            SpeechFailures.of(SpeechRecognizer.ERROR_SPEECH_TIMEOUT),
        )
    }

    @Test
    fun `a broken microphone is the same as not having one`() {
        assertEquals(SpeechFailure.UNAVAILABLE, SpeechFailures.of(SpeechRecognizer.ERROR_AUDIO))
    }

    @Test
    fun `any other code falls back to the generic failure`() {
        assertEquals(SpeechFailure.FAILED, SpeechFailures.of(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(SpeechFailure.FAILED, SpeechFailures.of(UNKNOWN_CODE))
    }

    @Test
    fun `the best hypothesis is the first one`() {
        val bundle = Bundle().apply {
            putStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION,
                arrayListOf("gasté 45 mil", "gaste 45 mil"),
            )
        }
        assertEquals("gasté 45 mil", SpeechFailures.bestOf(bundle))
    }

    @Test
    fun `nothing heard is null, and so is a blank hypothesis`() {
        assertNull(SpeechFailures.bestOf(null))
        assertNull(SpeechFailures.bestOf(Bundle()))
        val blank = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("   "))
        }
        assertNull(SpeechFailures.bestOf(blank))
    }

    private companion object {
        /** Un codigo que el sistema no define hoy: manana puede existir y no puede romper nada. */
        const val UNKNOWN_CODE = 99
    }
}
