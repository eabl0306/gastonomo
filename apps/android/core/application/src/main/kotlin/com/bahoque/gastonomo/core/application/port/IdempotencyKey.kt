package com.bahoque.gastonomo.core.application.port

import java.util.UUID

/**
 * La clave de idempotencia de los tres POST que crean dinero.
 *
 * La regla, y es la que mas facil es incumplir: **un valor por pulsacion de Guardar, el
 * mismo en todos los reintentos de ese intento, y nunca regenerado al reintentar**. Si se
 * regenera, el reintento crea un segundo registro y el diseno no tiene pantalla para
 * borrarlo.
 *
 * Se crea una vez cuando el usuario pulsa Guardar y se guarda en el estado del formulario:
 * ```
 * // en el ViewModel, al pulsar Guardar
 * val attempt = state.idempotencyKey ?: IdempotencyKey.newKey()
 * setState { copy(idempotencyKey = attempt) }
 * createMovement(draft, attempt)      // y el reintento usa la misma
 * // solo se borra cuando la creacion sale bien o el usuario cambia el formulario
 * ```
 *
 * Un 422 **no quema la clave**: se puede corregir el importe y reintentar con la misma.
 * Un 409 con [com.bahoque.gastonomo.core.application.error.ConflictCode.IDEMPOTENCY_KEY_REUSED]
 * significa que el cuerpo cambio, asi que hace falta una clave nueva.
 */
@JvmInline
value class IdempotencyKey(val value: String) {

    init {
        require(value.isNotBlank() && value.length <= MAX_LENGTH) {
            "An idempotency key is between 1 and $MAX_LENGTH characters"
        }
    }

    override fun toString(): String = value

    companion object {
        /** Maximo que acepta la API. */
        const val MAX_LENGTH = 200

        /** Una clave nueva para un intento nuevo. */
        fun newKey(): IdempotencyKey = IdempotencyKey(UUID.randomUUID().toString())
    }
}

/**
 * Resultado de una creacion idempotente.
 *
 * @property isReplay true cuando la API contesto con `Idempotent-Replay: true`, es decir
 *   cuando esto es un reintento de algo que ya se habia creado. **No hay que volver a
 *   pintar el exito**: el toast ya salio en el primer intento.
 */
data class IdempotentResult<T>(val value: T, val isReplay: Boolean)
