package com.bahoque.gastonomo.core.application.port

import java.time.LocalDate

/**
 * El dia de hoy, como puerto. El prototipo lo tiene fijo en 2026-09-22; en produccion es
 * la fecha real, y en los tests es la que el test decida.
 *
 * De aqui salen el mes por defecto del filtro de periodo, el limite del selector de mes
 * (no se puede pasar del mes actual), la fecha por defecto de los formularios y los
 * encabezados "Hoy" y "Ayer" de Movimientos.
 */
interface Clock {
    fun today(): LocalDate
}

/** El reloj del sistema, en la zona del dispositivo. */
object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
}
