package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * El bottom sheet del diseno: velo por encima de la pantalla, hoja `bgNav` con radio 28
 * arriba, handle de 40x4, cierre al tocar el velo y cierre arrastrando hacia abajo.
 *
 * No usa `ModalBottomSheet` de Material: ese trae su propio arrastre, su propia forma y su
 * propio velo, y el diseno fija los tres.
 *
 * **Va dentro de un [GastonomoSheetHost]**, que es quien la anima al abrir y al cerrar. Montada
 * sin host se pinta igual pero sin animacion, que es lo que hacen las capturas.
 *
 * ```
 * GastonomoSheetHost(visible = state.sheetOpen) {
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
                .sheetEntrance(LocalSheetTransition.current)
                .dismissOnDragDown(onDismiss)
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

/**
 * Hace que el panel suba al abrir y baje al cerrar, sincronizado con el fundido del velo que pone
 * [GastonomoSheetHost]. Sin host, o con las animaciones del sistema apagadas, no hace nada.
 */
@Composable
private fun Modifier.sheetEntrance(scope: AnimatedVisibilityScope?): Modifier {
    val motion = GastonomoTheme.motion
    if (scope == null || !motion.enabled) return this
    return with(scope) {
        this@sheetEntrance.animateEnterExit(
            enter = slideInVertically(motion.enterSpec()) { height -> height },
            exit = slideOutVertically(motion.exitSpec()) { height -> height },
        )
    }
}

/**
 * Cierra la hoja al arrastrarla hacia abajo, que es lo que el handle de 40x4 promete.
 *
 * Solo cuenta el arrastre hacia abajo: tirar hacia arriba no estira la hoja, se queda quieta. Y
 * se cierra tambien con un gesto corto pero rapido, porque exigir recorrido hace que un lanzamiento
 * normal no cierre y parezca que el gesto no existe.
 */
@Composable
private fun Modifier.dismissOnDragDown(onDismiss: () -> Unit): Modifier {
    val motion = GastonomoTheme.motion
    val dragged = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { DRAG_TO_DISMISS.toPx() }
    return this
        .offset { IntOffset(x = 0, y = dragged.value.roundToInt()) }
        .draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scope.launch { dragged.snapTo((dragged.value + delta).coerceAtLeast(0f)) }
            },
            onDragStopped = { velocity ->
                if (dragged.value > threshold || velocity > FLING_TO_DISMISS) {
                    onDismiss()
                } else {
                    dragged.animateTo(0f, motion.selectionSpec())
                }
            },
        )
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

/** Recorrido a partir del cual soltar cierra la hoja. */
private val DRAG_TO_DISMISS = 120.dp

/** Velocidad, en pixeles por segundo, a partir de la cual un gesto corto tambien cierra. */
private const val FLING_TO_DISMISS = 900f

private val TOP_PADDING = 10.dp
private val SIDE_PADDING = 20.dp
private val BOTTOM_PADDING = 26.dp
private val HANDLE_GAP = 14.dp
private val HEADER_GAP = 16.dp
private val TITLE_GAP = 4.dp
