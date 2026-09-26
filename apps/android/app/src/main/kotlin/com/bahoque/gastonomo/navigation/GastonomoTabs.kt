package com.bahoque.gastonomo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.TabItem
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.navigation.TopLevelDestination

/**
 * Las cuatro pestanas con su label traducido. El icono sale de
 * [TopLevelDestination], que no puede depender del sistema de diseno.
 */
@Composable
fun tabItems(): List<TabItem> = TopLevelDestination.entries.map { destination ->
    TabItem(icon = destination.icon, label = stringResource(destination.labelRes()))
}

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

private fun TopLevelDestination.labelRes(): Int = when (route) {
    GastonomoRoute.Home -> GastonomoStrings.tabHome
    GastonomoRoute.Movements -> GastonomoStrings.tabMovements
    GastonomoRoute.Loans -> GastonomoStrings.tabLoans
    else -> GastonomoStrings.tabSettings
}
