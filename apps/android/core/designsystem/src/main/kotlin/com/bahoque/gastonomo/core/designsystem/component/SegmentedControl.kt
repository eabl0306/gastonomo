package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El segmented control del diseno: contenedor `chipBg` con radio 16 y padding 4, segmento
 * de 42 con radio 12, blanco con sombra el activo y transparente en muted el inactivo.
 *
 * El texto es el mismo en los dos, 15 en bold: el prototipo solo cambia color y fondo. Si
 * el inactivo fuera mas fino, el control bailaria al cambiar de segmento.
 *
 * Con [compact] sale la variante del sheet de categoria, que el diseno pinta mas baja:
 * contenedor radio 14 con padding 3 y segmentos de 38 con radio 11 y texto de 14.
 *
 * ```
 * SegmentedControl(
 *     segments = listOf(Segment(expense, GastonomoIcons.NORTH_EAST, colors.red), Segment(...)),
 *     selectedIndex = state.kindIndex,
 *     onSelect = viewModel::selectKind,
 * )
 * ```
 */
@Composable
fun SegmentedControl(
    segments: List<Segment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val look = lookOf(compact)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(look.containerRadius))
            .background(GastonomoTheme.colors.chipBg)
            .padding(look.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            SegmentCell(
                segment = segment,
                selected = index == selectedIndex,
                look = look,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Los tokens que cambian entre el segmented de formulario y el del sheet de categoria. */
private data class SegmentedLook(
    val containerRadius: Dp,
    val containerPadding: Dp,
    val segmentHeight: Dp,
    val segmentRadius: Dp,
    val textStyle: TextStyle,
)

@Composable
private fun lookOf(compact: Boolean): SegmentedLook = if (compact) {
    SegmentedLook(
        containerRadius = COMPACT_CONTAINER_RADIUS,
        containerPadding = COMPACT_CONTAINER_PADDING,
        segmentHeight = COMPACT_SEGMENT_HEIGHT,
        segmentRadius = GastonomoTheme.radii.iconChoice,
        textStyle = GastonomoTheme.typography.segmentCompact,
    )
} else {
    SegmentedLook(
        containerRadius = GastonomoTheme.radii.control,
        containerPadding = CONTAINER_PADDING,
        segmentHeight = SEGMENT_HEIGHT,
        segmentRadius = SEGMENT_RADIUS,
        textStyle = GastonomoTheme.typography.segment,
    )
}

@Composable
private fun SegmentCell(
    segment: Segment,
    selected: Boolean,
    look: SegmentedLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(look.segmentRadius)
    val contentColor = when {
        !selected -> GastonomoTheme.colors.muted
        else -> segment.activeColor ?: GastonomoTheme.colors.ink
    }
    Box(
        modifier = modifier
            .height(look.segmentHeight)
            .then(
                if (selected) {
                    Modifier
                        .shadow(GastonomoTheme.shadows.activeSegment, shape)
                        .background(GastonomoTheme.colors.surface, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .pressable(onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (segment.icon != null) {
                GastonomoIcon(segment.icon, size = ICON_SIZE, tint = contentColor)
                Box(Modifier.width(GAP))
            }
            GastonomoText(
                text = segment.label,
                style = look.textStyle,
                color = contentColor,
                maxLines = 1,
            )
            if (segment.caption != null) {
                Box(Modifier.width(GAP))
                GastonomoText(
                    text = segment.caption,
                    style = GastonomoTheme.typography.secondarySmall,
                    color = GastonomoTheme.colors.faint,
                    maxLines = 1,
                )
            }
        }
    }
}

private val CONTAINER_PADDING = 4.dp
private val SEGMENT_HEIGHT = 42.dp
private val SEGMENT_RADIUS = 12.dp
private val COMPACT_CONTAINER_RADIUS = 14.dp
private val COMPACT_CONTAINER_PADDING = 3.dp
private val COMPACT_SEGMENT_HEIGHT = 38.dp
private val ICON_SIZE = 18.dp
private val GAP = 6.dp
