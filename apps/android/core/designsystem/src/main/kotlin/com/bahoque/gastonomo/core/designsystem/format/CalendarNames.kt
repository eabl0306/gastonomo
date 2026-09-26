package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.settings.AppLanguage

/**
 * Los nombres de meses y dias del prototipo, literales. No se sacan de `java.time`
 * porque los cortos del CLDR no coinciden: el diseno usa "sept" con cuatro letras y
 * "dic" sin punto, y una pantalla hi-fi no puede depender de la version de ICU del
 * telefono.
 */
internal class CalendarNames(
    val monthsLong: List<String>,
    val monthsShort: List<String>,
    val weekdaysLong: List<String>,
) {
    companion object {
        private val SPANISH = CalendarNames(
            monthsLong = words(
                "enero febrero marzo abril mayo junio " +
                    "julio agosto septiembre octubre noviembre diciembre",
            ),
            monthsShort = words("ene feb mar abr may jun jul ago sept oct nov dic"),
            weekdaysLong = words("domingo lunes martes miércoles jueves viernes sábado"),
        )

        private val ENGLISH = CalendarNames(
            monthsLong = words(
                "january february march april may june " +
                    "july august september october november december",
            ),
            monthsShort = words("Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec"),
            weekdaysLong = words("sunday monday tuesday wednesday thursday friday saturday"),
        )

        private fun words(names: String): List<String> = names.split(' ')

        fun of(language: AppLanguage): CalendarNames =
            if (language == AppLanguage.ENGLISH) ENGLISH else SPANISH
    }
}
