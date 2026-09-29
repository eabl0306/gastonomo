package com.bahoque.gastonomo.core.application.port

import kotlinx.coroutines.flow.Flow

/** Por que no se pudo escuchar. Cada caso tiene su mensaje y todos acaban en el modo escrito. */
enum class SpeechFailure {
    /** El dispositivo no trae reconocimiento de voz. */
    UNAVAILABLE,

    /** Falta el permiso del microfono. */
    PERMISSION,

    /** El reconocimiento del sistema necesita conexion y no hay. */
    OFFLINE,

    /** Se escucho, pero no se entendio nada. */
    NO_MATCH,

    /** Cualquier otro fallo del sistema. */
    FAILED,
}

/** Lo que va pasando mientras se escucha. */
sealed interface SpeechEvent {

    /**
     * Lo que se ha entendido hasta ahora. Se emite varias veces y cada una reemplaza a la
     * anterior: es el texto grande que se ve mientras el usuario habla.
     */
    data class Partial(val text: String) : SpeechEvent

    /** El usuario termino de hablar y esto es el texto definitivo. */
    data class Final(val text: String) : SpeechEvent

    /** No se pudo. */
    data class Failed(val reason: SpeechFailure) : SpeechEvent
}

/**
 * Una escucha en marcha.
 *
 * Es un objeto con estado y no un simple flujo porque terminar de escuchar **no es cancelar**:
 * el sistema todavia tiene que dar su resultado definitivo, que es mejor que el ultimo parcial.
 * Cancelar la recoleccion de [events], en cambio, corta sin resultado y suelta el microfono.
 */
interface SpeechSession {

    /** Termina cuando llega el resultado definitivo o un fallo. */
    val events: Flow<SpeechEvent>

    /** Pide al sistema que termine y de su resultado. El [SpeechEvent.Final] llega despues. */
    fun finish()
}

/**
 * El reconocimiento de voz del sistema.
 *
 * Es del sistema y no nuestro **a proposito**: ya sabe el idioma, funciona sin que la
 * aplicacion mande audio a ningun sitio y no cuesta nada. A la API solo llega el texto.
 */
interface SpeechToText {

    /** Si el dispositivo trae reconocimiento. Sin el, el dictado empieza en modo escrito. */
    fun isAvailable(): Boolean

    /**
     * Abre el microfono.
     *
     * @param languageTag el idioma en el que se habla, como `es-CO` o `en-US`.
     */
    fun start(languageTag: String): SpeechSession
}
