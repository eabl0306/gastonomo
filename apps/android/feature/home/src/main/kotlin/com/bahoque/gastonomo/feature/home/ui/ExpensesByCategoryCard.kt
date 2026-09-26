package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.RowDivider
import com.bahoque.gastonomo.core.designsystem.component.tintOfCategoryColor
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.analysis.CategorySlice
import com.bahoque.gastonomo.feature.home.HomeUiState
import com.bahoque.gastonomo.feature.home.R

/**
 * Gastos por categoria: la barra apilada de las seis categorias con mas gasto y una fila por
 * cada una con su icono, su porcentaje y su importe.
 *
 * Con el ojo cerrado los importes se sustituyen por cuatro puntos, que es lo que hace el
 * prototipo: la barra sigue siendo util porque son proporciones, no cifras.
 */
@Composable
internal fun ExpensesByCategoryCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val breakdown = state.content.breakdown
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        radius = GastonomoTheme.radii.loanCard,
        contentPadding = PaddingValues(CONTENT_PADDING),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            GastonomoText(
                text = stringResource(R.string.home_expenses_by_category),
                modifier = Modifier.weight(1f),
                style = GastonomoTheme.typography.sectionTitle,
                color = GastonomoTheme.colors.ink,
            )
            GastonomoAmountText(
                text = if (state.hideAmounts) {
                    GastonomoTheme.formats.masked(state.displayCurrency)
                } else {
                    GastonomoTheme.formats.amount(breakdown.total.amount, state.displayCurrency)
                },
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
            )
        }
        if (breakdown.slices.isEmpty()) {
            GastonomoText(
                text = stringResource(R.string.home_no_expenses),
                modifier = Modifier.padding(top = EMPTY_TOP, bottom = EMPTY_BOTTOM),
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.muted,
            )
            return@GastonomoCard
        }
        StackedBar(slices = breakdown.slices)
        breakdown.slices.forEach { slice ->
            RowDivider(horizontalPadding = 0.dp)
            SliceRow(slice = slice, hideAmounts = state.hideAmounts, state = state)
        }
    }
}

@Composable
private fun StackedBar(slices: List<CategorySlice>) {
    Row(
        // El padding va por fuera del alto: si se pone despues, se come los 12 de la barra.
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BAR_TOP, bottom = BAR_BOTTOM)
            .height(GastonomoTheme.dimens.stackedBar),
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
    ) {
        var covered = 0f
        slices.forEach { slice ->
            val fraction = slice.fraction.coerceAtLeast(MIN_FRACTION)
            covered += fraction
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxHeight()
                    .background(
                        color = categoryColorOf(slice.category.color),
                        shape = RoundedCornerShape(GastonomoTheme.radii.bar),
                    ),
            )
        }
        // Con mas de seis categorias la barra no llega al final: el diseno deja el hueco en
        // vez de repartir el resto entre las seis que si se ensenan.
        val rest = 1f - covered
        if (rest > MIN_FRACTION) {
            Spacer(modifier = Modifier.weight(rest))
        }
    }
}

@Composable
private fun SliceRow(slice: CategorySlice, hideAmounts: Boolean, state: HomeUiState) {
    val color = categoryColorOf(slice.category.color)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        IconTile(
            background = tintOfCategoryColor(color),
            size = TILE_SIZE,
            radius = GastonomoTheme.radii.iconTileTiny,
        ) {
            GastonomoIcon(name = slice.category.icon, size = TILE_ICON, tint = color)
        }
        GastonomoText(
            text = slice.category.name,
            modifier = Modifier.weight(1f),
            style = GastonomoTheme.typography.bodySmall,
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
        GastonomoAmountText(
            text = stringResource(R.string.home_category_percent, slice.percent),
            modifier = Modifier.width(PERCENT_WIDTH),
            style = GastonomoTheme.typography.secondarySmall.copy(textAlign = TextAlign.End),
            color = GastonomoTheme.colors.muted,
        )
        GastonomoAmountText(
            text = if (hideAmounts) {
                stringResource(R.string.home_masked_short)
            } else {
                GastonomoTheme.formats.amount(slice.total.amount, state.displayCurrency)
            },
            modifier = Modifier.width(AMOUNT_WIDTH),
            style = GastonomoTheme.typography.sliceAmount.copy(textAlign = TextAlign.End),
            color = GastonomoTheme.colors.ink,
        )
    }
}

/** Una porcion nunca desaparece del todo: sin esto un 0,2 por ciento no se veria. */
private const val MIN_FRACTION = 0.02f

private val CONTENT_PADDING = 18.dp
private val BAR_TOP = 14.dp
private val BAR_BOTTOM = 6.dp
private val BAR_GAP = 3.dp
private val ROW_PADDING = 9.dp
private val ROW_GAP = 10.dp
private val TILE_SIZE = 30.dp
private val TILE_ICON = 18.dp
private val PERCENT_WIDTH = 36.dp
private val AMOUNT_WIDTH = 92.dp
private val EMPTY_TOP = 18.dp
private val EMPTY_BOTTOM = 4.dp
