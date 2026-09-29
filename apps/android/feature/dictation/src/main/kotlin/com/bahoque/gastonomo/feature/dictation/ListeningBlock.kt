package com.bahoque.gastonomo.feature.dictation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La fase de escuchar: el microfono grande con su pulso, lo que se va entendiendo en grande y la
 * salida al modo escrito.
 *
 * Tocar el microfono **no cancela**: le pide al sistema su resultado definitivo, que es mejor que
 * el ultimo parcial.
 */
@Composable
internal fun ListeningBlock(state: DictateUiState, actions: DictateActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = TOP_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MicButton(onClick = actions.onStopListening)
        Spacer(Modifier.height(HEARD_GAP))
        GastonomoText(
            text = state.heard.ifBlank { stringResource(R.string.dictation_hearing) },
            style = GastonomoTheme.typography.greeting.copy(textAlign = TextAlign.Center),
            color = if (state.heard.isBlank()) {
                GastonomoTheme.colors.faint
            } else {
                GastonomoTheme.colors.ink
            },
        )
        Spacer(Modifier.height(HINT_GAP))
        GastonomoText(
            text = stringResource(R.string.dictation_tap_to_finish),
            style = GastonomoTheme.typography.secondary.copy(textAlign = TextAlign.Center),
            color = GastonomoTheme.colors.muted,
        )
        Spacer(Modifier.height(LINK_GAP))
        GastonomoText(
            text = stringResource(R.string.dictation_prefer_typing),
            modifier = Modifier.padding(LINK_PADDING).pressable(onClick = actions.onTypeInstead),
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.ink,
        )
    }
}

/**
 * El microfono con su pulso.
 *
 * El pulso late siempre, no con el volumen: sacar el nivel del microfono obliga a leer el audio, y
 * lo unico que esto tiene que decir es que se esta escuchando. Con las animaciones del sistema
 * apagadas se queda quieto, porque el token de movimiento vale cero.
 */
@Composable
private fun MicButton(onClick: () -> Unit) {
    val colors = GastonomoTheme.colors
    val side = GastonomoTheme.dimens.dictationMic
    val pulse = if (GastonomoTheme.motion.enabled) rememberPulse() else 0f
    Box(
        modifier = Modifier
            .size(side)
            .drawBehind {
                val full = size.minDimension / 2f
                drawCircle(
                    color = colors.lime,
                    radius = full * (PULSE_MIN + pulse * (1f - PULSE_MIN)),
                    alpha = PULSE_ALPHA * (1f - pulse),
                )
            }
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(side - MIC_INSET * 2)
                .background(colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GastonomoIcon(
                name = GastonomoIcons.MIC,
                size = MIC_ICON,
                tint = colors.lime,
                filled = true,
                contentDescription = stringResource(R.string.dictation_stop),
            )
        }
    }
}

/** El ciclo del pulso, de 0 a 1 y vuelta a empezar. */
@Composable
private fun rememberPulse(): Float {
    val transition = rememberInfiniteTransition(label = "dictation-pulse")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dictation-pulse-value",
    )
    return value
}

private val MIC_INSET = 10.dp
private val MIC_ICON = 40.dp
private val TOP_GAP = 20.dp
private val HEARD_GAP = 22.dp
private val HINT_GAP = 4.dp
private val LINK_GAP = 10.dp
private val LINK_PADDING = 6.dp

/** Lo que tarda un latido del pulso. Es largo a proposito: tiene que respirar, no parpadear. */
private const val PULSE_MILLIS = 1600

/** Radio del que arranca el pulso, en fraccion del circulo. */
private const val PULSE_MIN = 0.62f

/** Opacidad de la que parte, que se apaga a cero al llegar al borde. */
private const val PULSE_ALPHA = 0.55f
