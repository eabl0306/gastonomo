package com.bahoque.gastonomo.core.domain.settings

/**
 * Idioma de la aplicacion. Decide los textos, el formato de numeros, los nombres de
 * meses y dias y los nombres de monedas.
 */
enum class AppLanguage(val wire: String, val localeTag: String) {
    SPANISH("es", "es-CO"),
    ENGLISH("en", "en-US"),
    ;

    companion object {
        val DEFAULT = SPANISH

        fun fromWire(wire: String): AppLanguage? = entries.firstOrNull { it.wire == wire }
    }
}
