package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La tarjeta-radio del diseno: tarjeta blanca con titulo, subtitulo y un radio.
 * Seleccionada lleva borde ink de 1,5 dp.
 *
 * Tiene las dos formas que usa el diseno:
 * - [RadioCardLayout.Row], la del sheet de Idioma: icono, textos y radio en una fila.
 * - [RadioCardLayout.Stacked], la de las tarjetas de formato de Exportar: el icono arriba,
 *   los textos debajo y el radio en la esquina.
 *
 * ```
 * RadioCard(title = "Excel", subtitle = "...", selected = fmt == XLSX, onClick = { },
 *     layout = RadioCardLayout.Stacked) {
 *     IconTile(tintOfCategoryColor(green), radius = radii.iconChoice) { GastonomoIcon(TABLE_VIEW) }
 * }
 * ```
 */
@Composable
fun RadioCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    radius: Dp = GastonomoTheme.radii.smallCard,
    layout: RadioCardLayout = RadioCardLayout.Row,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(radius)
    val surface = modifier
        .fillMaxWidth()
        .background(GastonomoTheme.colors.surface, shape)
        .border(
            width = if (selected) GastonomoTheme.dimens.selectedBorder else GastonomoTheme.dimens.divider,
            color = if (selected) GastonomoTheme.colors.ink else GastonomoTheme.colors.line,
            shape = shape,
        )
        .pressable(onClick = onClick, role = Role.RadioButton)
    when (layout) {
        RadioCardLayout.Row -> Row(
            modifier = surface.padding(CONTENT_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            leading?.invoke()
            Titles(title, subtitle, Modifier.weight(1f))
            GastonomoRadio(selected = selected)
        }
        RadioCardLayout.Stacked -> Box(modifier = surface) {
            Column(modifier = Modifier.padding(CONTENT_PADDING)) {
                leading?.invoke()
                Titles(title, subtitle, Modifier.padding(top = STACKED_GAP))
            }
            GastonomoRadio(
                selected = selected,
                modifier = Modifier.align(Alignment.TopEnd).padding(RADIO_INSET),
            )
        }
    }
}

/** Como se colocan el icono, los textos y el radio de una [RadioCard]. */
enum class RadioCardLayout {
    /** Icono, textos y radio en una fila. Es la del sheet de Idioma. */
    Row,

    /** Icono arriba, textos debajo y radio en la esquina. Es la de Exportar. */
    Stacked,
}

@Composable
private fun Titles(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TITLE_GAP)) {
        GastonomoText(
            text = title,
            style = GastonomoTheme.typography.body,
            color = GastonomoTheme.colors.ink,
        )
        if (subtitle != null) {
            GastonomoText(
                text = subtitle,
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.muted,
            )
        }
    }
}

private val CONTENT_PADDING = 14.dp
private val GAP = 12.dp
private val TITLE_GAP = 2.dp
private val STACKED_GAP = 10.dp
private val RADIO_INSET = 12.dp
