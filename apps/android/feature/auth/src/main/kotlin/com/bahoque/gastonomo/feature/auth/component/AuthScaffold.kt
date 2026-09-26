package com.bahoque.gastonomo.feature.auth.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El armazon de las tres pantallas de autenticacion con formulario: fondo, los 24 dp de
 * margen lateral del diseno, el boton de vuelta opcional y el pie anclado abajo.
 *
 * El contenido va en una columna con scroll y el pie fuera de ella. En el prototipo el pie
 * se aparta con un `flex:1` dentro del area que hace scroll; aqui queda fijo, asi que con
 * la pantalla pequena o con la fuente del sistema muy grande el formulario se desplaza y
 * el pie sigue visible en vez de recortarse.
 *
 * ```
 * AuthScaffold(onBack = onBack, footer = { AuthFooterPrompt(...) }) {
 *     AuthHeader(title = ..., subtitle = ...)
 *     GastonomoTextField(...)
 * }
 * ```
 */
@Composable
internal fun AuthScaffold(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    topPadding: Dp = STACKED_TOP_PADDING,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GastonomoScreen(
        modifier = modifier.navigationBarsPadding().imePadding(),
        horizontalPadding = GastonomoTheme.dimens.authScreenPadding,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = topPadding),
        ) {
            if (onBack != null) {
                AuthBackButton(onBack = onBack)
            }
            content()
        }
        footer?.invoke()
        Spacer(Modifier.height(BOTTOM_PADDING))
    }
}

/**
 * El `arrow_back` de Registro y Recuperar. No es una app bar: el diseno solo pone el boton
 * suelto, desplazado a la izquierda para que el glifo quede alineado con el texto.
 */
@Composable
private fun AuthBackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = -BACK_BUTTON_OFFSET)
            .size(GastonomoTheme.dimens.minTouchTarget)
            .pressable(onClick = onBack, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            GastonomoIcons.ARROW_BACK,
            size = BACK_ICON_SIZE,
            tint = GastonomoTheme.colors.ink,
            contentDescription = stringResource(GastonomoStrings.actionBack),
        )
    }
}

/** Login no lleva boton de vuelta y empieza mas abajo. */
internal val LOGIN_TOP_PADDING = 28.dp

/** Las pantallas apiladas arrancan pegadas arriba porque el boton de vuelta ya separa. */
private val STACKED_TOP_PADDING = 8.dp

private val BOTTOM_PADDING = 24.dp
private val BACK_BUTTON_OFFSET = 10.dp
private val BACK_ICON_SIZE = 24.dp
