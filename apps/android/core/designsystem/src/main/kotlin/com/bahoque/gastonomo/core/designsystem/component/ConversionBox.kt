package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.format.AmountInput
import com.bahoque.gastonomo.core.designsystem.format.GastonomoFormats
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoFontFamily
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.ConversionSuggestion

/**
 * La caja de conversion de los tres formularios de creacion: movimiento, prestamo y abono.
 *
 * Solo se ensena cuando el registro esta en una moneda distinta de la principal. Propone la
 * equivalencia con la tasa vigente, deja editarla y ofrece "Usar sugerida" para volver. El
 * calculo esta en [ConversionSuggestion]; aqui solo se pinta.
 *
 * ```
 * if (state.suggestion.isForeign) {
 *     ConversionBox(
 *         suggestion = state.suggestion,
 *         baseRaw = state.baseRaw,
 *         onBaseChange = viewModel::onBase,
 *         onUseSuggested = viewModel::onUseSuggestedRate,
 *     )
 * }
 * ```
 *
 * @param baseRaw lo que el usuario escribio, con punto decimal, o null para la sugerida.
 */
@Composable
fun ConversionBox(
    suggestion: ConversionSuggestion,
    baseRaw: String?,
    onBaseChange: (String) -> Unit,
    onUseSuggested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = GastonomoTheme.formats
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        radius = GastonomoTheme.radii.smallCard,
        contentPadding = PaddingValues(horizontal = CONTENT_PADDING, vertical = VERTICAL_PADDING),
    ) {
        Header(code = suggestion.display.value, edited = suggestion.isEdited, onUseSuggested = onUseSuggested)
        BaseField(
            symbol = formats.symbolOf(suggestion.display),
            shown = formats.amountInput.display(baseRaw ?: suggestedRaw(suggestion)),
            onTyped = { typed -> onBaseChange(formats.amountInput.sanitize(typed, suggestion.display)) },
        )
        GastonomoText(
            text = helpText(suggestion, formats),
            style = GastonomoTheme.typography.secondarySmall,
            color = GastonomoTheme.colors.muted,
            modifier = Modifier.padding(top = HELP_GAP),
        )
    }
}

@Composable
private fun Header(code: String, edited: Boolean, onUseSuggested: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoText(
            text = stringResource(R.string.gastonomo_conversion_header, code),
            modifier = Modifier.weight(1f),
            style = GastonomoTheme.typography.secondarySmall.copy(fontWeight = FontWeight.SemiBold),
            color = GastonomoTheme.colors.muted,
        )
        if (edited) {
            TextLink(
                text = stringResource(R.string.gastonomo_conversion_use_suggested),
                onClick = onUseSuggested,
                style = GastonomoTheme.typography.secondarySmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun BaseField(symbol: String, shown: String, onTyped: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = FIELD_GAP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FIELD_GAP_INNER),
    ) {
        GastonomoAmountText(
            text = symbol,
            style = GastonomoTheme.typography.amountCard,
            color = GastonomoTheme.colors.ink,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (shown.isEmpty()) {
                GastonomoText(
                    text = stringResource(R.string.gastonomo_amount_placeholder),
                    style = BASE_STYLE,
                    color = GastonomoTheme.colors.faint,
                )
            }
            BasicTextField(
                value = shown,
                onValueChange = onTyped,
                singleLine = true,
                textStyle = BASE_STYLE.copy(color = GastonomoTheme.colors.ink),
                cursorBrush = SolidColor(GastonomoTheme.colors.ink),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GastonomoIcon(
            name = GastonomoIcons.EDIT,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.faint,
            contentDescription = stringResource(R.string.gastonomo_conversion_edit),
        )
    }
}

/** La equivalencia que se pinta cuando el usuario todavia no ha escrito nada. */
private fun suggestedRaw(suggestion: ConversionSuggestion): String {
    if (suggestion.entered.amount.signum() <= 0) return ""
    val suggested = suggestion.suggestedBase ?: return ""
    return AmountInput.toRaw(suggested, suggestion.display)
}

/** Las tres variantes del texto de ayuda, en el orden que decide el prototipo. */
@Composable
private fun helpText(suggestion: ConversionSuggestion, formats: GastonomoFormats): String {
    val code = suggestion.entered.currency.value
    val display = suggestion.display.value
    val ownRate = suggestion.ownRate.takeIf { suggestion.isEdited }
    val suggested = suggestion.suggestedRate
    return when {
        ownRate != null && suggested != null -> stringResource(
            R.string.gastonomo_conversion_rate_own,
            code,
            formats.rate(ownRate),
            display,
            formats.rate(suggested),
        )
        ownRate != null -> stringResource(
            R.string.gastonomo_conversion_rate_own_only,
            code,
            formats.rate(ownRate),
            display,
        )
        suggested != null -> stringResource(
            R.string.gastonomo_conversion_rate_suggested,
            code,
            formats.rate(suggested),
            display,
        )
        else -> stringResource(R.string.gastonomo_conversion_rate_missing, code)
    }
}

/** El input de la equivalencia: 22/700 con cifras de ancho fijo, como fija el diseno. */
private val BASE_STYLE = TextStyle(
    fontFamily = GastonomoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
).withTabularNumbers()

private val CONTENT_PADDING = 14.dp
private val VERTICAL_PADDING = 12.dp
private val FIELD_GAP = 4.dp
private val FIELD_GAP_INNER = 6.dp
private val HELP_GAP = 4.dp
private val ICON_SIZE = 18.dp
