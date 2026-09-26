package com.bahoque.gastonomo.core.navigation

/**
 * Las cuatro pantallas que llevan tab bar, en el orden del diseno.
 *
 * @property icon el nombre del Material Symbol, el mismo que usa
 *   [com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons] aunque este modulo no lo
 *   importe: las rutas no pueden depender del sistema de diseno.
 */
enum class TopLevelDestination(val route: GastonomoRoute, val icon: String) {
    HOME(GastonomoRoute.Home, "home"),
    MOVEMENTS(GastonomoRoute.Movements, "swap_vert"),
    LOANS(GastonomoRoute.Loans, "handshake"),
    SETTINGS(GastonomoRoute.Settings, "settings"),
}
