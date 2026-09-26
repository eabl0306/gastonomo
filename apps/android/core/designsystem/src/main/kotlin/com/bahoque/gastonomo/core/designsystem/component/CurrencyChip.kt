package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * El chip "{COD} - {Nombre}" que abre el sheet de moneda debajo del monto, en los tres
 * formularios de creacion: movimiento, prestamo y abono.
 *
 * El nombre sale localizado del CLDR del dispositivo, asi que en ingles el mismo chip pone
 * "Colombian Peso". [fallbackName] es el nombre que mando la API, por si el dispositivo no
 * conoce el codigo.
 *
 * ```
 * CurrencyChip(currency = state.currency, onClick = viewModel::openCurrencySheet)
 * ```
 */
@Composable
fun CurrencyChip(
    currency: CurrencyCode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackName: String = currency.value,
) {
    val label = currency.value + SEPARATOR + GastonomoTheme.formats.currencyNames.of(currency, fallbackName)
    val shape = RoundedCornerShape(GastonomoTheme.radii.control)
    Box(
        // El chip del diseno mide 32 de alto. El area que responde al toque se estira a los
        // 44 minimos de Android sin mover el dibujo, que es lo unico que se ve.
        modifier = modifier
            .heightIn(min = GastonomoTheme.dimens.minTouchTarget)
            .pressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(CHIP_HEIGHT)
                .background(GastonomoTheme.colors.surface, shape)
                .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.inputBorder, shape)
                .padding(start = START_PADDING, end = END_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            GastonomoText(
                text = label,
                style = GastonomoTheme.typography.secondary.copy(fontWeight = FontWeight.SemiBold),
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            GastonomoIcon(GastonomoIcons.EXPAND_MORE, size = ICON_SIZE, tint = GastonomoTheme.colors.ink)
        }
    }
}

private const val SEPARATOR = " · "
private val CHIP_HEIGHT = 32.dp
private val START_PADDING = 12.dp
private val END_PADDING = 8.dp
private val GAP = 2.dp
private val ICON_SIZE = 18.dp
