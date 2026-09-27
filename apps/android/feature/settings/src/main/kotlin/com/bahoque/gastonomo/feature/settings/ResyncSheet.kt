package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.component.SheetHeader
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La confirmacion de borrar lo local y bajarlo otra vez.
 *
 * Pide confirmacion porque la operacion borra datos, tarda y quiere red estable. Y **si hay
 * escrituras sin subir no ofrece el boton**: borrar con cosas sin subir es perder movimientos del
 * usuario, que es el peor fallo posible de esta aplicacion.
 */
@Composable
internal fun ResyncSheet(
    pendingWrites: Int,
    running: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GastonomoBottomSheet(onDismiss = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.settings_resync_title),
            subtitle = stringResource(R.string.settings_resync_explains),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GAP),
        ) {
            if (pendingWrites > 0) {
                GastonomoText(
                    text = pluralStringResource(
                        R.plurals.settings_resync_blocked,
                        pendingWrites,
                        pendingWrites,
                    ),
                    style = GastonomoTheme.typography.secondary,
                    color = GastonomoTheme.colors.amber,
                )
                SecondaryButton(
                    text = stringResource(GastonomoStrings.actionClose),
                    onClick = onDismiss,
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.settings_resync_confirm),
                    onClick = onConfirm,
                    enabled = !running,
                )
                SecondaryButton(
                    text = stringResource(GastonomoStrings.actionCancel),
                    onClick = onDismiss,
                    enabled = !running,
                )
            }
        }
    }
}

private val GAP = 10.dp
