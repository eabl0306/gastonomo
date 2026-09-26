package com.bahoque.gastonomo.feature.auth.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R

/**
 * El teclado 3x4 del diseno: teclas de 80x68 con radio 22, huella abajo a la izquierda y
 * borrar abajo a la derecha, las dos sin fondo.
 *
 * Una tecla pulsada cambia de fondo en vez de escalarse, que es lo que hace el prototipo.
 */
@Composable
internal fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        DIGIT_ROWS.forEach { row ->
            KeyRow {
                row.forEach { digit -> DigitKey(digit = digit, onClick = { onDigit(digit) }) }
            }
        }
        KeyRow {
            IconKey(
                icon = GastonomoIcons.FINGERPRINT,
                description = stringResource(R.string.auth_unlock_fingerprint),
                onClick = onBiometrics,
            )
            DigitKey(digit = LAST_DIGIT, onClick = { onDigit(LAST_DIGIT) })
            IconKey(
                icon = GastonomoIcons.BACKSPACE,
                description = stringResource(R.string.auth_unlock_backspace),
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP)) { content() }
}

@Composable
private fun DigitKey(digit: Char, onClick: () -> Unit) {
    KeySurface(background = GastonomoTheme.colors.chipBg, onClick = onClick) {
        GastonomoText(
            text = digit.toString(),
            style = GastonomoTheme.typography.keypadDigit,
            color = GastonomoTheme.colors.ink,
        )
    }
}

@Composable
private fun IconKey(icon: String, description: String, onClick: () -> Unit) {
    KeySurface(background = Color.Transparent, onClick = onClick) {
        GastonomoIcon(
            icon,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.ink,
            contentDescription = description,
        )
    }
}

@Composable
private fun KeySurface(
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val fill = when {
        !pressed -> background
        background == Color.Transparent -> GastonomoTheme.colors.chipBg
        else -> GastonomoTheme.colors.inputBorder
    }
    Box(
        modifier = Modifier
            .width(GastonomoTheme.dimens.keypadKeyWidth)
            .height(GastonomoTheme.dimens.keypadKeyHeight)
            .background(fill, RoundedCornerShape(GastonomoTheme.radii.keypadKey))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private val DIGIT_ROWS = listOf("123", "456", "789")
private const val LAST_DIGIT = '0'
private val ROW_GAP = 14.dp
private val COLUMN_GAP = 26.dp
private val ICON_SIZE = 28.dp
