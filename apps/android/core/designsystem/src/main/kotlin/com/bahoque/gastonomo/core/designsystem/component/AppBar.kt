package com.bahoque.gastonomo.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La app bar de las pantallas secundarias: 52 dp de alto, boton de vuelta a la izquierda
 * y titulo centrado.
 *
 * El icono de vuelta es `arrow_back` en las pantallas de consulta y `close` en los
 * formularios de creacion, que es lo que hace [AppBarNavigation].
 *
 * ```
 * GastonomoAppBar(
 *     title = stringResource(R.string.new_movement),
 *     navigation = AppBarNavigation.Close,
 *     onNavigate = ::dismiss,
 * )
 * ```
 */
@Composable
fun GastonomoAppBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    navigation: AppBarNavigation = AppBarNavigation.Back,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GastonomoTheme.dimens.appBar)
            .padding(horizontal = SIDE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        Box(
            modifier = Modifier
                .size(GastonomoTheme.dimens.minTouchTarget)
                .pressable(onClick = onNavigate, role = Role.Button),
            contentAlignment = Alignment.Center,
        ) {
            GastonomoIcon(
                name = navigation.icon,
                tint = GastonomoTheme.colors.ink,
                size = ICON_SIZE,
                contentDescription = stringResource(navigation.description),
            )
        }
        // El titulo va pegado al boton, no centrado: las cinco pantallas secundarias del
        // diseno lo ponen a la izquierda.
        GastonomoText(
            text = title,
            modifier = Modifier.weight(1f),
            style = GastonomoTheme.typography.appBarTitle,
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
        actions?.let {
            Row(verticalAlignment = Alignment.CenterVertically) { it() }
        }
    }
}

/**
 * Como se sale de una pantalla secundaria. Cada forma lleva su texto para el lector de
 * pantalla: un icono suelto no dice nada.
 */
enum class AppBarNavigation(val icon: String, @StringRes val description: Int) {
    /** Pantalla de consulta: se vuelve atras. */
    Back(GastonomoIcons.ARROW_BACK, GastonomoStrings.actionBack),

    /** Formulario de creacion: se descarta. */
    Close(GastonomoIcons.CLOSE, GastonomoStrings.actionClose),
}

private val ICON_SIZE = 24.dp
private val SIDE_PADDING = 12.dp
private val GAP = 6.dp
