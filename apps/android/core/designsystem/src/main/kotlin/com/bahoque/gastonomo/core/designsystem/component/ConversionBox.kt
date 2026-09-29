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
 * Solo se ensena cuando el registro esta en una moneda distinta de la principal. **Lo que se
 * teclea es la tasa**, y la equivalencia se calcula y se cuenta debajo. Antes era al reves, y
 * era pedirle al usuario la cuenta ya hecha: quien recibe dolares sabe a como esta el cambio,
 * no cuanto suman ciento cuarenta dolares en pesos. El calculo esta en [ConversionSuggestion];
 * aqui solo se pinta.
 *
 * ```
 * if (state.suggestion.isForeign) {
 *     ConversionBox(
 *         suggestion = state.suggestion,
 *         rateRaw = state.rateRaw,
 *         onRateChange = viewModel::onRate,
 *         onUseSuggested = { viewModel.onRate(null) },
 *     )
 * }
 * ```
 *
 * @param rateRaw la tasa que escribio el usuario, con punto decimal, o null para la sugerida.
 */
@Composable
fun ConversionBox(
    suggestion: ConversionSuggestion,
    rateRaw: String?,
    onRateChange: (String) -> Unit,
    onUseSuggested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = GastonomoTheme.formats
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        radius = GastonomoTheme.radii.smallCard,
        contentPadding = PaddingValues(horizontal = CONTENT_PADDING, vertical = VERTICAL_PADDING),
    ) {
        Header(
            from = suggestion.entered.currency.value,
            to = suggestion.display.value,
            edited = suggestion.isEdited,
            onUseSuggested = onUseSuggested,
        )
        RateField(
            code = suggestion.display.value,
            shown = formats.amountInput.display(rateRaw ?: suggestedRateRaw(suggestion)),
            onTyped = { typed -> onRateChange(formats.amountInput.sanitizeRate(typed)) },
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
private fun Header(from: String, to: String, edited: Boolean, onUseSuggested: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoText(
            text = stringResource(R.string.gastonomo_conversion_header, from, to),
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

/** La tasa: el numero se teclea y al lado va el codigo de la moneda principal. */
@Composable
private fun RateField(code: String, shown: String, onTyped: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = FIELD_GAP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FIELD_GAP_INNER),
    ) {
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
        GastonomoText(
            text = code,
            style = GastonomoTheme.typography.secondarySmall.copy(fontWeight = FontWeight.SemiBold),
            color = GastonomoTheme.colors.muted,
        )
        GastonomoIcon(
            name = GastonomoIcons.EDIT,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.faint,
            contentDescription = stringResource(R.string.gastonomo_conversion_edit),
        )
    }
}

/** La tasa que se pinta cuando el usuario todavia no ha escrito ninguna. */
private fun suggestedRateRaw(suggestion: ConversionSuggestion): String {
    val suggested = suggestion.suggestedRate ?: return ""
    return AmountInput.rateToRaw(suggested)
}

/**
 * El texto de debajo cuenta **la equivalencia**, que es lo que el usuario ya no teclea. Cuando
 * cambio la tasa se anade la sugerida al lado, que es la referencia para saber si se paso.
 */
@Composable
private fun helpText(suggestion: ConversionSuggestion, formats: GastonomoFormats): String {
    val suggested = suggestion.suggestedRate
    if (suggestion.rate == null) {
        return stringResource(R.string.gastonomo_conversion_rate_missing, suggestion.entered.currency.value)
    }
    val equivalent = formats.amount(suggestion.base, suggestion.display)
    return when {
        suggestion.isEdited && suggested != null -> stringResource(
            R.string.gastonomo_conversion_equivalent_own,
            equivalent,
            formats.rate(suggested),
        )
        suggestion.isEdited -> stringResource(R.string.gastonomo_conversion_equivalent_own_only, equivalent)
        else -> stringResource(R.string.gastonomo_conversion_equivalent_suggested, equivalent)
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
