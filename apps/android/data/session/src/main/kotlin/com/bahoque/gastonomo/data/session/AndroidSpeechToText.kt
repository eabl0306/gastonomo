package com.bahoque.gastonomo.data.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.bahoque.gastonomo.core.application.port.SpeechEvent
import com.bahoque.gastonomo.core.application.port.SpeechFailure
import com.bahoque.gastonomo.core.application.port.SpeechSession
import com.bahoque.gastonomo.core.application.port.SpeechToText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * El reconocimiento de voz del sistema.
 *
 * Vive en este modulo, y no en uno propio, porque aqui estan los demas adaptadores que hablan
 * con el dispositivo y no con la API: la huella y el monitor de red.
 *
 * Dos cosas del `SpeechRecognizer` que no se ven en su firma y rompen el dictado si se olvidan:
 * **se crea y se maneja desde el hilo principal**, y **desde Android 11 hace falta declarar la
 * consulta del servicio de reconocimiento en el manifiesto** o `isRecognitionAvailable` contesta
 * siempre que no hay.
 */
internal class AndroidSpeechToText(private val context: Context) : SpeechToText {

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(languageTag: String): SpeechSession =
        AndroidSpeechSession(context, languageTag)
}

/**
 * Una escucha del sistema.
 *
 * El flujo se cierra solo al llegar el resultado definitivo o un fallo, asi que quien lo recoge
 * no tiene que acordarse de cortarlo. Y al cerrarse **libera el reconocedor**: dejarlo vivo
 * mantiene el microfono abierto y el aviso de grabacion encendido.
 */
private class AndroidSpeechSession(
    private val context: Context,
    private val languageTag: String,
) : SpeechSession {

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var recognizer: SpeechRecognizer? = null

    /** True si se pidio terminar antes de que el reconocedor estuviera en marcha. */
    @Volatile
    private var finishRequested = false

    override val events: Flow<SpeechEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechEvent.Failed(SpeechFailure.UNAVAILABLE))
            close()
            return@callbackFlow
        }
        val started = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = started
        started.setRecognitionListener(
            listener { event ->
                trySend(event)
                if (event !is SpeechEvent.Partial) close()
            },
        )
        started.startListening(intentFor(languageTag))
        if (finishRequested) started.stopListening()
        awaitClose {
            recognizer = null
            started.cancel()
            started.destroy()
        }
    }.flowOn(Dispatchers.Main)

    override fun finish() {
        finishRequested = true
        main.post { recognizer?.stopListening() }
    }

    /**
     * Solo interesan tres momentos de los diez que tiene el listener: lo que se va entendiendo,
     * el texto definitivo y el error. El resto son medidas del volumen y avisos de inicio y fin
     * de audio, y no cambian nada de lo que se ve.
     */
    private fun listener(emit: (SpeechEvent) -> Unit): RecognitionListener =
        object : SilentRecognitionListener() {

            override fun onPartialResults(partialResults: Bundle?) {
                SpeechFailures.bestOf(partialResults)?.let { emit(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                emit(SpeechEvent.Final(SpeechFailures.bestOf(results).orEmpty()))
            }

            override fun onError(error: Int) {
                emit(SpeechEvent.Failed(SpeechFailures.of(error)))
            }
        }

    private fun intentFor(tag: String) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
}

/**
 * Los seis metodos del listener que no usamos, en un sitio. Implementarlos en la sesion la
 * dejaba por encima del limite de lineas sin anadir nada que se lea.
 */
private abstract class SilentRecognitionListener : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
