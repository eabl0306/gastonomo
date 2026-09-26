package com.bahoque.gastonomo.feature.auth

/**
 * Lo que se comprueba antes de llamar a la API.
 *
 * El prototipo no valida nada y el handoff avisa de que en produccion si hay que hacerlo.
 * La API tambien valida y devuelve 422 con el mensaje en castellano, pero validar aqui
 * ahorra el viaje y marca el campo en el momento.
 *
 * El correo no se comprueba con `android.util.Patterns` para que estas reglas se puedan
 * probar en la JVM sin Robolectric.
 */
internal object AuthValidation {

    /** El minimo que exige la API. El medidor de fuerza es solo visual. */
    const val MINIMUM_PASSWORD_LENGTH = 8

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

    fun isLongEnough(password: String): Boolean = password.length >= MINIMUM_PASSWORD_LENGTH

    /** Algo antes de la arroba, algo despues y un dominio con punto. Sin espacios. */
    private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}")
}
