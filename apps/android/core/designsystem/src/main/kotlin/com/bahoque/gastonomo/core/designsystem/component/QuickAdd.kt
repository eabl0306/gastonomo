package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons

/**
 * El boton de registro rapido de la aplicacion, con sus tres opciones ya puestas.
 *
 * Existe para que las tres pantallas que lo llevan, Inicio, Movimientos y Prestamos, no repitan
 * los mismos iconos y los mismos textos: si manana cambia una opcion, cambia en un sitio.
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     GastonomoScreen { ... }
 *     GastonomoQuickAdd(onNewMovement = ..., onNewLoan = ..., onDictate = ...)
 * }
 * ```
 */
@Composable
fun BoxScope.GastonomoQuickAdd(
    onNewMovement: () -> Unit,
    onNewLoan: () -> Unit,
    onDictate: () -> Unit,
) {
    GastonomoRadialFab(
        options = listOf(
            RadialOption(
                icon = GastonomoIcons.SWAP_VERT,
                label = stringResource(GastonomoStrings.quickAddMovement),
                onSelect = onNewMovement,
            ),
            RadialOption(
                icon = GastonomoIcons.HANDSHAKE,
                label = stringResource(GastonomoStrings.quickAddLoan),
                onSelect = onNewLoan,
            ),
            RadialOption(
                icon = GastonomoIcons.MIC,
                label = stringResource(GastonomoStrings.quickAddDictate),
                onSelect = onDictate,
            ),
        ),
        closeLabel = stringResource(GastonomoStrings.quickAddClose),
        openLabel = stringResource(GastonomoStrings.quickAdd),
    )
}
