package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.LocalContentColor

/**
 * El fondo de una pantalla, con el padding horizontal del diseno ya puesto.
 *
 * ```
 * GastonomoScreen { /* contenido, ya con los 20 dp de margen */ }
 * GastonomoScreen(horizontalPadding = GastonomoTheme.dimens.authScreenPadding) { ... }
 * ```
 */
@Composable
fun GastonomoScreen(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = GastonomoTheme.dimens.screenPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GastonomoTheme.colors.bgApp)
            .padding(horizontal = horizontalPadding),
        content = content,
    )
}

/**
 * Una tarjeta blanca con borde, el contenedor de casi todo el diseno.
 *
 * @param border null para una tarjeta sin borde, como las sub tarjetas de la de balance.
 */
@Composable
fun GastonomoCard(
    modifier: Modifier = Modifier,
    background: Color = GastonomoTheme.colors.surface,
    border: Color? = GastonomoTheme.colors.line,
    radius: Dp = GastonomoTheme.radii.card,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentColor: Color = GastonomoTheme.colors.ink,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(GastonomoTheme.dimens.divider, border, shape) else Modifier)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = { content() })
    }
}

/**
 * Un tile de icono cuadrado y redondeado. Es el prefijo de casi todas las filas del
 * diseno: 40 dp con radio 13 en las listas, 38 con radio 12 en Categorias.
 *
 * El fondo de un tile de categoria es su color al 9 por ciento, que es lo que devuelve
 * [tintOfCategoryColor].
 */
@Composable
fun IconTile(
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = GastonomoTheme.dimens.iconTile,
    radius: Dp = GastonomoTheme.radii.iconTile,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(background),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** El tinte del tile de una categoria: su color con alfa 0x18, como en el prototipo. */
fun tintOfCategoryColor(color: Color): Color = color.copy(alpha = CATEGORY_TINT_ALPHA)

/** Alfa 0x18 sobre 0xFF. */
private const val CATEGORY_TINT_ALPHA = 24f / 255f
