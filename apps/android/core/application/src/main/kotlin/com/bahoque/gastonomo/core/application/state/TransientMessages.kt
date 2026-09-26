package com.bahoque.gastonomo.core.application.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * La clave de un aviso. Es una clave y no un texto a proposito: el texto vive en el
 * `strings.xml` de la pantalla que lo pinta, asi que el nucleo no guarda castellano.
 */
@JvmInline
value class MessageKey(val value: String) {
    override fun toString(): String = value
}

/**
 * El toast del diseno, que sobrevive a un cambio de pantalla.
 *
 * Hace falta porque los formularios de creacion guardan, **vuelven atras** y el toast sale
 * encima de la pantalla de destino, que es otra: el formulario ya no esta en pantalla para
 * pintarlo. Hay un unico aviso a la vez, igual que en el prototipo.
 *
 * ```
 * // al guardar, antes de volver atras
 * messages.post(TransientMessages.MOVEMENT_SAVED_EXPENSE)
 *
 * // en la pantalla de destino
 * val message by messages.message.collectAsStateWithLifecycle()
 * message?.let { GastonomoToast(textOf(it), onHide = messages::consume) }
 * ```
 *
 * Quien anada una pantalla que avisa declara su propia clave: [MessageKey] es publico y no
 * hace falta tocar este fichero.
 */
class TransientMessages {

    private val mutable = MutableStateFlow<MessageKey?>(null)

    /** El aviso pendiente, o null si no hay ninguno. */
    val message: StateFlow<MessageKey?> = mutable.asStateFlow()

    /** Deja un aviso para la siguiente pantalla que lo pinte. */
    fun post(key: MessageKey) {
        mutable.value = key
    }

    /** Lo llama la pantalla cuando el toast se oculta, a los 2,2 segundos. */
    fun consume() {
        mutable.value = null
    }

    companion object {
        /** "Ingreso guardado". */
        val MOVEMENT_SAVED_INCOME = MessageKey("movement_saved_income")

        /** "Gasto guardado". */
        val MOVEMENT_SAVED_EXPENSE = MessageKey("movement_saved_expense")
    }
}
