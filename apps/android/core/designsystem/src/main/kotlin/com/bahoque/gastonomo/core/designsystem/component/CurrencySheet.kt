package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * El sheet de moneda, compartido por Ajustes y por los tres formularios de dinero. Vive en
 * el sistema de diseno porque una feature no puede depender de otra y las cuatro pantallas
 * necesitan exactamente este.
 *
 * Filtra por codigo o por nombre, ensena la seccion Frecuentes mientras no se busca y pinta
 * el nombre de cada moneda en el idioma vigente.
 *
 * ```
 * if (state.sheet == Sheet.Currency) {
 *     CurrencySheet(
 *         origin = CurrencySheetOrigin.Settings,
 *         currencies = state.currencies,
 *         selected = state.displayCurrency,
 *         query = state.query,
 *         onQueryChange = viewModel::onCurrencyQuery,
 *         onSelect = { viewModel.onSettingChanged(currency = it) },
 *         onDismiss = viewModel::closeSheet,
 *     )
 * }
 * ```
 */
@Composable
fun CurrencySheet(
    origin: CurrencySheetOrigin,
    currencies: List<CurrencyInfo>,
    selected: CurrencyCode,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (CurrencyCode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = GastonomoTheme.formats.displayCurrency.value
    val matches = remember(currencies, query) { filterCurrencies(currencies, query) }
    val frequent = remember(matches, query) {
        if (query.isBlank()) matches.filter { it.code in CurrencyCode.FREQUENT } else emptyList()
    }
    val rest = remember(matches, frequent) { matches - frequent.toSet() }
    GastonomoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        SheetHeader(title = stringResource(origin.titleRes()), subtitle = origin.subtitle(display))
        GastonomoTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.gastonomo_currency_search),
            height = SEARCH_HEIGHT,
            radius = GastonomoTheme.radii.monthPicker,
            contentPadding = SEARCH_PADDING,
            leading = {
                GastonomoIcon(GastonomoIcons.SEARCH, tint = GastonomoTheme.colors.faint)
            },
        )
        LazyColumn(modifier = Modifier.padding(top = LIST_GAP).heightIn(max = LIST_MAX_HEIGHT)) {
            if (frequent.isNotEmpty()) {
                item { SectionLabel(R.string.gastonomo_currency_frequent) }
                item {
                    CurrencyCard(frequent, selected, onSelect, Modifier.padding(bottom = CARD_GAP))
                }
                if (rest.isNotEmpty()) item { SectionLabel(R.string.gastonomo_currency_all) }
            }
            // Sin coincidencias hay que pintar la tarjeta igualmente: lleva "Sin resultados.".
            if (rest.isNotEmpty() || matches.isEmpty()) {
                item { CurrencyCard(rest, selected, onSelect) }
            }
            item { CurrencyFooter(currencies.size) }
        }
    }
}

@Composable
private fun CurrencyCard(
    rows: List<CurrencyInfo>,
    selected: CurrencyCode,
    onSelect: (CurrencyCode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.smallCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GastonomoTheme.colors.surface)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.line, shape),
    ) {
        if (rows.isEmpty()) {
            GastonomoText(
                text = stringResource(R.string.gastonomo_currency_empty),
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.muted,
                modifier = Modifier.padding(horizontal = EMPTY_SIDE_PADDING, vertical = EMPTY_PADDING),
            )
        }
        rows.forEachIndexed { index, currency ->
            CurrencyRow(
                currency = currency,
                selected = currency.code == selected,
                showDivider = index != rows.lastIndex,
                onClick = { onSelect(currency.code) },
            )
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: CurrencyInfo,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .background(if (selected) GastonomoTheme.colors.iconTile else Color.Transparent)
                .pressable(onClick = onClick, role = Role.RadioButton)
                .padding(horizontal = ROW_SIDE_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            GastonomoText(
                text = currency.code.value,
                modifier = Modifier.width(CODE_WIDTH),
                style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = GastonomoTheme.colors.ink,
            )
            GastonomoText(
                text = GastonomoTheme.formats.currencyNames.of(currency.code, currency.name),
                modifier = Modifier.weight(1f),
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            GastonomoText(
                text = GastonomoTheme.formats.symbolOf(currency.code),
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
            )
            Box(modifier = Modifier.width(CHECK_WIDTH), contentAlignment = Alignment.Center) {
                if (selected) {
                    GastonomoIcon(
                        name = GastonomoIcons.CHECK,
                        tint = GastonomoTheme.colors.ink,
                        contentDescription = stringResource(R.string.gastonomo_currency_selected),
                    )
                }
            }
        }
        if (showDivider) {
            RowDivider(horizontalPadding = ROW_SIDE_PADDING)
        }
    }
}

@Composable
private fun SectionLabel(labelRes: Int) {
    GastonomoText(
        text = stringResource(labelRes).uppercase(),
        style = GastonomoTheme.typography.overline.copy(fontSize = SMALL_LABEL_SIZE),
        color = GastonomoTheme.colors.muted,
        modifier = Modifier.padding(horizontal = LABEL_SIDE_PADDING, vertical = LABEL_GAP),
    )
}

@Composable
private fun CurrencyFooter(total: Int) {
    GastonomoText(
        text = pluralStringResource(R.plurals.gastonomo_currency_footer, total, total),
        style = GastonomoTheme.typography.secondarySmall
            .copy(fontSize = SMALL_LABEL_SIZE, textAlign = TextAlign.Center),
        color = GastonomoTheme.colors.faint,
        modifier = Modifier.fillMaxWidth().padding(top = FOOTER_GAP),
    )
}

/** Filtra por codigo o por nombre, sin distinguir mayusculas, como hace el prototipo. */
internal fun filterCurrencies(currencies: List<CurrencyInfo>, query: String): List<CurrencyInfo> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return currencies
    return currencies.filter {
        it.code.value.lowercase().contains(needle) || it.name.lowercase().contains(needle)
    }
}

private fun CurrencySheetOrigin.titleRes(): Int = when (this) {
    CurrencySheetOrigin.Settings -> R.string.gastonomo_currency_title_settings
    CurrencySheetOrigin.Record -> R.string.gastonomo_currency_title_record
}

@Composable
private fun CurrencySheetOrigin.subtitle(display: String): String = when (this) {
    CurrencySheetOrigin.Settings -> stringResource(R.string.gastonomo_currency_subtitle_settings)
    CurrencySheetOrigin.Record -> stringResource(R.string.gastonomo_currency_subtitle_record, display)
}

private val SEARCH_HEIGHT = 46.dp

/** El buscador es el unico input que el diseno sangra 14 en vez de 16. */
private val SEARCH_PADDING = 14.dp
private val LIST_MAX_HEIGHT = 380.dp
private val LIST_GAP = 12.dp
private val CARD_GAP = 14.dp
private val ROW_HEIGHT = 54.dp
private val ROW_SIDE_PADDING = 10.dp
private val ROW_GAP = 12.dp
private val CODE_WIDTH = 44.dp
private val CHECK_WIDTH = 20.dp
private val LABEL_SIDE_PADDING = 4.dp
private val LABEL_GAP = 6.dp
private val FOOTER_GAP = 10.dp
private val EMPTY_PADDING = 18.dp
private val EMPTY_SIDE_PADDING = 14.dp
private val SMALL_LABEL_SIZE = 11.sp
