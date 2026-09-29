package com.bahoque.gastonomo.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.TabItem

/**
 * Las cuatro pestanas con su label traducido, en el orden del diseno.
 *
 * Vive aqui y no en `:app` porque lo usan dos sitios: la aplicacion y las capturas de pantalla,
 * que montan el mismo armazon. Con una copia por lado, renombrar una pestana o anadir una
 * quinta dejaba las capturas ensenando otra cosa que la aplicacion.
 */
@Composable
fun tabItems(): List<TabItem> = TopLevelDestination.entries.map { destination ->
    TabItem(icon = destination.icon, label = stringResource(destination.labelRes()))
}

/**
 * El texto de cada pestana.
 *
 * El `when` es sobre la pestana y **no lleva `else`**: asi el compilador obliga a poner texto a
 * la siguiente que se anada. Con `else` se rotulaba sola como "Ajustes" y nada fallaba.
 */
@StringRes
private fun TopLevelDestination.labelRes(): Int = when (this) {
    TopLevelDestination.HOME -> GastonomoStrings.tabHome
    TopLevelDestination.MOVEMENTS -> GastonomoStrings.tabMovements
    TopLevelDestination.LOANS -> GastonomoStrings.tabLoans
    TopLevelDestination.SETTINGS -> GastonomoStrings.tabSettings
}
