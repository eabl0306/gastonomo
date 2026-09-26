package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La barra de progreso de un prestamo: fondo `track` y relleno ink si esta abierto o
 * verde si esta pagado. 6 dp en la tarjeta y 8 dp en el detalle.
 *
 * ```
 * ProgressBar(fraction = summary.percent / 100f, fill = if (paid) colors.green else colors.ink)
 * ```
 */
@Composable
fun ProgressBar(
    fraction: Float,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Dp = GastonomoTheme.dimens.progressBar,
    track: Color = GastonomoTheme.colors.track,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(track, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(fill, CircleShape),
        )
    }
}
