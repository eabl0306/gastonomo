package com.bahoque.gastonomo.core.application.error

/**
 * Lo que puede salir mal en una llamada a la API, ya traducido de `problem+json` a algo
 * que el nucleo entiende. Todos los puertos devuelven `Outcome<T, ApiError>`.
 */
sealed interface ApiError {

    /** 422. Trae los errores por campo, que van marcados en el formulario. */
    data class Validation(val fields: List<FieldError>, val detail: String? = null) : ApiError {
        /** El mensaje de un campo, o null si el 422 no hablaba de el. */
        fun messageFor(field: String): String? = fields.firstOrNull { it.field == field }?.message
    }

    /** 409. El `code` dice cual de los conflictos es; estan en [ConflictCode]. */
    data class Conflict(val code: String?, val detail: String? = null) : ApiError

    /** 401 de login: correo o contrasena incorrectos. */
    data object InvalidCredentials : ApiError

    /** 401 con sesion: el token no vale y el refresco no lo pudo arreglar. */
    data object Unauthorized : ApiError

    /** 404. Tambien es lo que responde un recurso de otra cuenta: nunca un 403. */
    data object NotFound : ApiError

    /** 429. Pasa en register, login y password/forgot. */
    data class TooManyRequests(val retryAfterSeconds: Long? = null) : ApiError

    /**
     * 503. No es un fallo: falta un dato que llena una tarea de fondo, asi que se
     * reintenta. Pasa en `GET /v1/exchange-rates` sin ninguna pasada guardada y en
     * `PATCH /v1/settings` con el catalogo de monedas vacio.
     */
    data object ServiceUnavailable : ApiError

    /** No se pudo hablar con la API: sin red, DNS, timeout. */
    data object Network : ApiError

    /** Cualquier otra cosa, con el estado si lo hubo. */
    data class Unexpected(val status: Int? = null, val detail: String? = null) : ApiError
}

/** Los `code` que la API pone en un 409. */
object ConflictCode {
    /** La misma clave de idempotencia con otro cuerpo: no es un reintento. */
    const val IDEMPOTENCY_KEY_REUSED = "idempotency_key_reused"

    /** Dos intentos con la misma clave a la vez: hay que esperar y reintentar. */
    const val IDEMPOTENCY_KEY_IN_PROGRESS = "idempotency_key_in_progress"

    /** El nombre de categoria ya existe en la cuenta, sin distinguir tipo ni mayusculas. */
    const val CATEGORY_NAME_TAKEN = "category_name_taken"

    /** Se intento editar o borrar una de las 12 predeterminadas. */
    const val CATEGORY_IS_DEFAULT = "category_is_default"
}
