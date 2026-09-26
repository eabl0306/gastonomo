package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R

/**
 * Las cuatro barras de 4 dp y su etiqueta. Rojo en el nivel 1, ambar en el 2 y verde en el
 * 3 y el 4; las barras que faltan quedan en el color de borde de un input.
 *
 * El lector de pantalla lee una sola frase, "Fuerza de la contrasena: Buena", en vez de
 * cuatro rectangulos sin nombre.
 */
@Composable
internal fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(strength.label)
    val description = stringResource(R.string.auth_strength_label, label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = BARS_TOP_PADDING, bottom = BARS_BOTTOM_PADDING),
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        ) {
            repeat(TOTAL_BARS) { index ->
                Bar(
                    color = if (index < strength.filledBars) {
                        strength.fillColor()
                    } else {
                        GastonomoTheme.colors.inputBorder
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        GastonomoText(
            text = label,
            style = GastonomoTheme.typography.secondarySmall,
            color = GastonomoTheme.colors.muted,
        )
    }
}

@Composable
private fun Bar(color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(BAR_HEIGHT)
            // La barra mide 4 y el diseno la redondea 2: un pill exacto.
            .background(color, CircleShape),
    )
}

@Composable
private fun PasswordStrength.fillColor(): Color = when (this) {
    PasswordStrength.EMPTY -> GastonomoTheme.colors.inputBorder
    PasswordStrength.VERY_SHORT -> GastonomoTheme.colors.red
    PasswordStrength.ALMOST -> GastonomoTheme.colors.amber
    PasswordStrength.GOOD, PasswordStrength.STRONG -> GastonomoTheme.colors.green
}

private const val TOTAL_BARS = 4
private val BAR_HEIGHT = 4.dp
private val BAR_GAP = 4.dp
private val BARS_TOP_PADDING = 10.dp
private val BARS_BOTTOM_PADDING = 4.dp
