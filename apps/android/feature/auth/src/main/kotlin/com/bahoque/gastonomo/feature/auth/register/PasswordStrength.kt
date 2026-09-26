package com.bahoque.gastonomo.feature.auth.register

import androidx.annotation.StringRes
import com.bahoque.gastonomo.feature.auth.R

/**
 * El medidor de fuerza de la contrasena: cuatro barras y una etiqueta.
 *
 * Es **solo visual**: la API exige nada mas el minimo de ocho caracteres y el resto de los
 * niveles no bloquean el registro. Los umbrales son los del prototipo, que es la fuente de
 * verdad: menos de 6 es nivel 1, menos de 8 es nivel 2, 8 o mas es nivel 3, y sube a 4 si
 * ademas mezcla letra y numero.
 *
 * @property filledBars cuantas de las cuatro barras se pintan.
 */
enum class PasswordStrength(val filledBars: Int, @get:StringRes val label: Int) {
    /** Sin escribir nada: ninguna barra y el texto de ayuda. */
    EMPTY(0, R.string.auth_strength_empty),

    /** Menos de seis caracteres. Rojo. */
    VERY_SHORT(1, R.string.auth_strength_very_short),

    /** Seis o siete caracteres. Ambar. */
    ALMOST(2, R.string.auth_strength_almost),

    /** Ocho o mas, pero solo letras o solo numeros. Verde. */
    GOOD(3, R.string.auth_strength_good),

    /** Ocho o mas con letra y numero. Verde. */
    STRONG(4, R.string.auth_strength_strong),
    ;

    companion object {
        fun of(password: String): PasswordStrength = when {
            password.isEmpty() -> EMPTY
            password.length < SHORT_LENGTH -> VERY_SHORT
            password.length < MINIMUM_LENGTH -> ALMOST
            password.any { it.isDigit() } && password.any { it.isLetter() } -> STRONG
            else -> GOOD
        }

        private const val SHORT_LENGTH = 6
        private const val MINIMUM_LENGTH = 8
    }
}
