package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.port.Clock
import java.time.LocalDate

/**
 * Un reloj que no avanza. Por defecto devuelve el 22 de septiembre de 2026, que es el
 * "hoy" del prototipo: con esa fecha las capturas salen iguales al diseno.
 */
class FixedClock(private val date: LocalDate = PROTOTYPE_TODAY) : Clock {
    override fun today(): LocalDate = date

    companion object {
        /** El "hoy" fijo del prototipo. */
        val PROTOTYPE_TODAY: LocalDate = LocalDate.of(2026, 9, 22)
    }
}
