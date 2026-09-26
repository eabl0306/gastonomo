package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El bottom sheet del diseno: velo por encima de la pantalla, hoja `bgNav` con radio 28
 * arriba, handle de 40x4 y cierre al tocar el velo.
 *
 * No usa `ModalBottomSheet` de Material: ese trae su propio arrastre, su propia forma y su
 * propio velo, y el diseno fija los tres.
 *
 * ```
 * if (state.sheet == Sheet.Range) {
 *     GastonomoBottomSheet(onDismiss = viewModel::closeSheet) {
 *         SheetHeader(title = ..., subtitle = ...)
 *         // contenido
 *     }
 * }
 * ```
 */
@Composable
fun GastonomoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GastonomoTheme.colors.scrim)
            .pressable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Consume el toque para que no llegue al velo y cierre la hoja.
                .pressable(onClick = {})
                .background(
                    color = GastonomoTheme.colors.bgNav,
                    shape = RoundedCornerShape(
                        topStart = GastonomoTheme.radii.sheet,
                        topEnd = GastonomoTheme.radii.sheet,
                    ),
                )
                .padding(top = TOP_PADDING, start = SIDE_PADDING, end = SIDE_PADDING, bottom = BOTTOM_PADDING),
        ) {
            SheetHandle()
            content()
        }
    }
}

/** El handle de 40x4 centrado en la parte de arriba de la hoja. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(bottom = HANDLE_GAP),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(GastonomoTheme.dimens.sheetHandleWidth)
                .height(GastonomoTheme.dimens.sheetHandleHeight)
                .background(GastonomoTheme.colors.toggleOff, CircleShape),
        )
    }
}

/** El titulo y el subtitulo de una hoja, con el espaciado del diseno. */
@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = HEADER_GAP),
        verticalArrangement = Arrangement.spacedBy(TITLE_GAP),
    ) {
        GastonomoText(
            text = title,
            style = GastonomoTheme.typography.sheetTitle,
            color = GastonomoTheme.colors.ink,
        )
        if (subtitle != null) {
            GastonomoText(
                text = subtitle,
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
            )
        }
    }
}

private val TOP_PADDING = 10.dp
private val SIDE_PADDING = 20.dp
private val BOTTOM_PADDING = 26.dp
private val HANDLE_GAP = 14.dp
private val HEADER_GAP = 16.dp
private val TITLE_GAP = 4.dp
