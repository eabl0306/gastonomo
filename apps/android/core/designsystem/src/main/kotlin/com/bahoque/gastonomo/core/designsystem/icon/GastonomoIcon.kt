package com.bahoque.gastonomo.core.designsystem.icon

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.LocalContentColor
import com.bahoque.gastonomo.core.designsystem.theme.MaterialSymbolsRounded
import com.bahoque.gastonomo.core.designsystem.theme.MaterialSymbolsRoundedFilled

/**
 * Un icono del diseno. Se pinta escribiendo su nombre como texto: la fuente Material
 * Symbols Rounded lo convierte en glifo por ligadura, igual que hace el prototipo.
 *
 * Se eligio la fuente y no `material-icons-extended` porque el diseno usa los glifos
 * Rounded y el eje FILL, y esa biblioteca no tiene ni una cosa ni la otra. La fuente
 * completa pesa 15 MB, asi que va recortada a los iconos de [GastonomoIcons]: 19 KB en
 * FILL 0 y 26 KB en FILL 1.
 *
 * ```
 * GastonomoIcon(GastonomoIcons.HANDSHAKE, size = 20.dp, tint = GastonomoTheme.colors.blue)
 * GastonomoIcon(GastonomoIcons.HOME, filled = true)   // el icono del tab activo
 * ```
 *
 * @param name uno de los nombres de [GastonomoIcons].
 * @param filled true para el eje FILL 1, que el diseno solo usa en el tab activo.
 * @param contentDescription null si el icono es decorativo y el texto de al lado ya lo dice.
 */
@Composable
fun GastonomoIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = DefaultIconSize,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    contentDescription: String? = null,
) {
    val fontSize: TextUnit = with(LocalDensity.current) { size.toSp() }
    BasicText(
        text = name,
        modifier = modifier.clearAndSetSemantics {
            contentDescription?.let { this.contentDescription = it }
        },
        style = TextStyle(
            fontFamily = if (filled) MaterialSymbolsRoundedFilled else MaterialSymbolsRounded,
            fontSize = fontSize,
            color = tint,
            // El glifo ocupa exactamente su caja: sin esto la fuente anade interlineado y
            // el icono deja de estar centrado en su tile.
            lineHeight = fontSize,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}

/** Tamano de icono por defecto, el de una fila de lista. */
val DefaultIconSize: Dp = 20.dp
