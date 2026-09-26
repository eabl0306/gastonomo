package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import java.math.BigDecimal

/**
 * La tendencia del balance de la tarjeta de Inicio: el acumulado dia a dia del periodo hasta
 * hoy, en linea lime de 2 px sobre un area al 12 por ciento, con la linea cero punteada.
 *
 * Se dibuja con Canvas y no con un SVG porque el diseno solo necesita dos trazos; el calculo
 * de los puntos es del dominio
 * ([com.bahoque.gastonomo.core.domain.analysis.BalanceTrend]) y aqui solo se escala.
 *
 * La escala incluye siempre el cero, como en el prototipo: si no, un periodo que solo sube
 * dibujaria la linea cero fuera de la caja.
 */
@Composable
internal fun BalanceTrendChart(
    points: List<BigDecimal>,
    modifier: Modifier = Modifier,
) {
    val values = paddedValues(points)
    val lineColor = GastonomoTheme.colors.lime
    val zeroColor = GastonomoTheme.colors.balance.zeroLine
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
    ) {
        val minimum = minOf(0f, values.min())
        val maximum = maxOf(0f, values.max())
        val span = (maximum - minimum).takeIf { it > 0f } ?: 1f
        val inset = VERTICAL_INSET.toPx()
        val usable = size.height - inset * 2
        fun y(value: Float): Float = inset + (1f - (value - minimum) / span) * usable
        val step = size.width / (values.size - 1)
        val line = Path().apply {
            values.forEachIndexed { index, value ->
                val x = index * step
                if (index == 0) moveTo(x, y(value)) else lineTo(x, y(value))
            }
        }
        // El orden es el del prototipo: la linea cero va debajo del area, no encima.
        drawLine(
            color = zeroColor,
            start = Offset(0f, y(0f)),
            end = Offset(size.width, y(0f)),
            strokeWidth = ZERO_STROKE.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx()),
            ),
        )
        drawPath(
            path = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = lineColor,
            alpha = AREA_ALPHA,
        )
        drawPath(
            path = line,
            color = lineColor,
            // El diseno redondea las uniones, no los extremos: con inglete un pico sale afilado.
            style = Stroke(width = LINE_STROKE.toPx(), join = StrokeJoin.Round),
        )
    }
}

/**
 * El prototipo dibuja dos puntos como minimo: con uno solo no hay linea que trazar, asi que
 * antepone un cero.
 */
private fun paddedValues(points: List<BigDecimal>): List<Float> {
    val values = points.map { it.toFloat() }
    return when (values.size) {
        0 -> listOf(0f, 0f)
        1 -> listOf(0f, values.first())
        else -> values
    }
}

/** El alto de 64 del diseno. El ancho lo da el contenedor. */
private val CHART_HEIGHT = 64.dp
private val VERTICAL_INSET = 4.dp
private val LINE_STROKE = 2.dp
private val ZERO_STROKE = 1.dp
private val DASH_ON = 3.dp
private val DASH_OFF = 4.dp
private const val AREA_ALPHA = 0.12f
