package com.bahoque.gastonomo.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.AmountInputField
import com.bahoque.gastonomo.core.designsystem.component.ConversionBox
import com.bahoque.gastonomo.core.designsystem.component.CurrencyChip
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.SYMBOL_SIZE
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * El bloque de importe que comparten Nuevo prestamo y Registrar abono: el monto centrado,
 * su leyenda, el chip de moneda y la caja de conversion cuando la moneda no es la principal.
 *
 * Los tres componentes son del sistema de diseno; aqui solo se apilan con el espaciado del
 * prototipo, que es el mismo en las dos pantallas salvo el tamano del monto.
 *
 * @param caption la leyenda bajo el monto, "Monto total" en Nuevo prestamo y nada en el
 *   sheet de abono.
 */
@Composable
fun MoneyEntryBlock(
    entry: MoneyEntryState,
    context: MoneyContext,
    color: Color,
    actions: MoneyEntryActions,
    modifier: Modifier = Modifier,
    amountStyle: TextStyle = GastonomoTheme.typography.amountInput,
    symbolSize: Int = SYMBOL_SIZE,
    caption: String? = null,
    topPadding: Dp = FORM_TOP_PADDING,
) {
    val suggestion = entry.suggestion(context)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = BOTTOM_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AmountInputField(
            raw = entry.amountRaw,
            onRawChange = actions.onAmount,
            currency = entry.currency,
            color = color,
            amountStyle = amountStyle,
            symbolSize = symbolSize,
        )
        if (caption != null) {
            GastonomoText(
                text = caption,
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.muted,
            )
        }
        CurrencyChip(
            currency = entry.currency,
            onClick = { actions.onCurrencySheet(true) },
            fallbackName = fallbackNameOf(entry.currency, context),
            enabled = entry.canConvert,
            disabledHint = stringResource(GastonomoStrings.offlineCurrency),
        )
        // Sin poder convertir no hay nada que ensenar en la caja: el registro va en la moneda
        // principal y su equivalencia es el propio importe.
        if (suggestion.isForeign && entry.canConvert) {
            ConversionBox(
                suggestion = suggestion,
                rateRaw = entry.rateRaw,
                onRateChange = actions.onRate,
                onUseSuggested = { actions.onRate(null) },
                modifier = Modifier.padding(top = CONVERSION_GAP),
            )
        }
    }
}

/** El nombre que mando la API, por si el CLDR del dispositivo no conoce el codigo. */
private fun fallbackNameOf(currency: CurrencyCode, context: MoneyContext): String =
    context.currencies.firstOrNull { it.code == currency }?.name ?: currency.value

/** El hueco sobre el monto en un formulario de pantalla completa. */
private val FORM_TOP_PADDING = 28.dp
private val BOTTOM_PADDING = 14.dp

/**
 * El chip se dibuja dentro de un area de toque de 44, asi que ya aporta 6 por arriba: el
 * hueco de 14 del diseno sobre la caja de conversion se queda en 8.
 */
private val CONVERSION_GAP = 8.dp
