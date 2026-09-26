package com.bahoque.gastonomo.core.domain.user

/**
 * El usuario de la sesion. Alimenta el saludo de Inicio, las iniciales del avatar y la
 * tarjeta de perfil de Ajustes.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
) {
    /** Las iniciales del avatar: una o dos letras en mayuscula. */
    val initials: String
        get() = name.trim().split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { email.take(1).uppercase() }

    /** El primer nombre, que es lo que usa "Hola, {nombre}". */
    val firstName: String get() = name.trim().split(WHITESPACE).firstOrNull().orEmpty()

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
