package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El encabezado de un grupo: 12 dp en negrita, en mayusculas y con tracking amplio. Es lo
 * que separa Cuenta, General y Datos en Ajustes, y Mis categorias de Predeterminadas.
 *
 * El texto se pone en mayusculas aqui y no en `strings.xml`, para que la traduccion siga
 * siendo legible y para que cada idioma use su propia regla de mayusculas.
 *
 * ```
 * Overline(stringResource(R.string.settings_group_account))
 * ```
 */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    GastonomoText(
        text = text.uppercase(),
        modifier = modifier.padding(horizontal = SIDE_PADDING),
        style = GastonomoTheme.typography.overline,
        color = GastonomoTheme.colors.muted,
    )
}

private val SIDE_PADDING = 4.dp
