package com.bahoque.gastonomo.data.session

import android.os.Bundle
import android.speech.SpeechRecognizer
import com.bahoque.gastonomo.core.application.port.SpeechFailure

/**
 * Lo que se puede probar del dictado sin microfono: traducir los codigos de error del sistema y
 * quedarse con la mejor hipotesis del resultado.
 *
 * Esta fuera de [AndroidSpeechToText] a proposito. Esa clase no se puede ejecutar en la JVM y
 * esta excluida de la cobertura; meter aqui el `when` de los errores dejaria sin probar la unica
 * parte que decide lo que el usuario lee cuando el dictado falla.
 */
internal object SpeechFailures {

    /** El codigo del sistema, traducido al motivo que la pantalla sabe explicar. */
    fun of(error: Int): SpeechFailure = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechFailure.PERMISSION
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> SpeechFailure.OFFLINE
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> SpeechFailure.NO_MATCH
        SpeechRecognizer.ERROR_AUDIO -> SpeechFailure.UNAVAILABLE
        else -> SpeechFailure.FAILED
    }

    /**
     * La primera hipotesis, que es la que el sistema da como mas probable. null si no hay
     * ninguna o viene en blanco.
     */
    fun bestOf(bundle: Bundle?): String? = bundle
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
}
