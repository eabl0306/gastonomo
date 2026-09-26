package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.format.AmountInput
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * El monto de un formulario de creacion: el simbolo de la moneda y, al lado, el importe en
 * 44/700 del color del tipo de registro (rojo en un gasto, verde en un ingreso, azul o ambar
 * en un prestamo).
 *
 * El formato es en vivo: lo que se teclea se limpia con
 * [com.bahoque.gastonomo.core.designsystem.format.AmountInput] y se ensena con el separador
 * de miles del idioma, asi que el estado guarda el valor raw y no lo que se ve.
 *
 * ```
 * AmountInputField(
 *     raw = state.amountRaw,
 *     onRawChange = viewModel::onAmount,
 *     currency = state.currency,
 *     color = if (state.kind.isIncome) colors.green else colors.red,
 * )
 * ```
 *
 * @param symbolSize 40 en movimiento y prestamo, 38 en abono, que es lo que fija el diseno.
 */
@Composable
fun AmountInputField(
    raw: String,
    onRawChange: (String) -> Unit,
    currency: CurrencyCode,
    color: Color,
    modifier: Modifier = Modifier,
    amountStyle: TextStyle = GastonomoTheme.typography.amountInput,
    symbolSize: Int = SYMBOL_SIZE,
) {
    val formats = GastonomoTheme.formats
    val style = amountStyle.withTabularNumbers()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        GastonomoText(
            text = formats.symbolOf(currency),
            style = symbolStyleFor(amountStyle, symbolSize),
            color = color,
            maxLines = 1,
        )
        Box(modifier = Modifier.width(FIELD_WIDTH)) {
            if (raw.isEmpty()) {
                GastonomoText(
                    text = stringResource(R.string.gastonomo_amount_placeholder),
                    style = style,
                    color = GastonomoTheme.colors.faint,
                    maxLines = 1,
                )
            }
            val shown = formats.amountInput.display(raw)
            // Solo el cursor se guarda aqui: el texto sale siempre de `raw`, que es el estado.
            var caret by remember { mutableStateOf(shown.length) }
            BasicTextField(
                value = TextFieldValue(shown, TextRange(caret.coerceAtMost(shown.length))),
                onValueChange = { typed ->
                    val sanitized = formats.amountInput.sanitize(typed.text, currency)
                    caret = caretFor(typed, formats.amountInput.display(sanitized))
                    onRawChange(sanitized)
                },
                singleLine = true,
                textStyle = style.copy(color = color),
                cursorBrush = SolidColor(color),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
    }
}

/**
 * Donde queda el cursor despues de reescribir el texto. Escribiendo al final se queda al final;
 * editando el medio se queda detras del mismo digito, aunque se haya movido un separador.
 */
private fun caretFor(typed: TextFieldValue, shown: String): Int =
    if (typed.selection.end == typed.text.length) {
        shown.length
    } else {
        AmountInput.caretAfter(shown, typed.text.take(typed.selection.end).count(Char::isDigit))
    }

/** El tamano del simbolo en el monto grande de movimiento y prestamo. */
const val SYMBOL_SIZE = 40

/** El ancho del campo del monto, el mismo del prototipo. */
private val FIELD_WIDTH = 230.dp
private val GAP = 4.dp
