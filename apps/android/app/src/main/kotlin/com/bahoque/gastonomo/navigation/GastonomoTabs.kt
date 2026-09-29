package com.bahoque.gastonomo.navigation

import androidx.navigation.NavDestination
import com.bahoque.gastonomo.core.navigation.TopLevelDestination

/**
 * Cual de las cuatro pestanas esta activa, o -1 si la pantalla actual es secundaria. Con
 * -1 la tab bar no se ensena: el diseno solo la pone en Inicio, Movimientos, Prestamos y
 * Ajustes.
 */
fun selectedTabIndex(destination: NavDestination?): Int {
    // Con rutas tipadas, `route` es el nombre cualificado de la clase, mas los argumentos
    // detras si los lleva. Las cuatro principales no llevan ninguno.
    val current = destination?.route?.substringBefore('/')?.substringBefore('?') ?: return -1
    return TopLevelDestination.entries.indexOfFirst { it.route::class.qualifiedName == current }
}
