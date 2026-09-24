package com.bahoque.gastonomo.core.common

/**
 * Resultado explicito de una operacion. El nucleo lo usa en vez de excepciones para que
 * los errores formen parte de la firma de los puertos.
 */
sealed interface Outcome<out V, out E> {

    data class Success<out V>(val value: V) : Outcome<V, Nothing>

    data class Failure<out E>(val error: E) : Outcome<Nothing, E>

    val isSuccess: Boolean get() = this is Success
}

inline fun <V, E, R> Outcome<V, E>.fold(onSuccess: (V) -> R, onFailure: (E) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}

inline fun <V, E, R> Outcome<V, E>.map(transform: (V) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <V, E, R> Outcome<V, E>.flatMap(transform: (V) -> Outcome<R, E>): Outcome<R, E> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

inline fun <V, E, F> Outcome<V, E>.mapError(transform: (E) -> F): Outcome<V, F> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Failure(transform(error))
}

fun <V, E> Outcome<V, E>.valueOrNull(): V? = (this as? Outcome.Success)?.value

fun <V, E> Outcome<V, E>.errorOrNull(): E? = (this as? Outcome.Failure)?.error
