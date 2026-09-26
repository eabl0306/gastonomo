package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.core.application.port.SystemClock
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.state.UserPreferences

/**
 * El estado que dos pantallas distintas comparten y que por eso no puede vivir en un
 * ViewModel. Se construye una vez y se reparte por los ficheros de `wiring`.
 *
 * - [sharedPeriodFilter]: el filtro de fechas. El diseno dice que el periodo elegido se ve
 *   igual en Inicio y en Movimientos, y son dos modulos que no se conocen.
 * - [sharedMessages]: el toast. Un formulario guarda, vuelve atras y el aviso sale encima de
 *   la pantalla de destino, que es otra.
 * - [sharedUserPreferences]: el idioma y la moneda principal. Los aplica el tema, que esta por
 *   encima de todas las pantallas.
 *
 * Quien necesite uno de los dos lo anade a las dependencias de su feature y lo rellena desde
 * aqui, como hacen `HomeWiring` y `MovementsWiring`.
 */
internal val sharedPeriodFilter: PeriodFilter = PeriodFilter.startingOn(SystemClock.today())

/** El unico toast de la aplicacion, compartido por todas las pantallas. */
internal val sharedMessages: TransientMessages = TransientMessages()

/**
 * El idioma y la moneda principal que la API confirmo. Los rellenan los casos de uso de
 * ajustes y los lee la raiz de composicion para pasarselos al tema.
 */
internal val sharedUserPreferences: UserPreferences = UserPreferences()
