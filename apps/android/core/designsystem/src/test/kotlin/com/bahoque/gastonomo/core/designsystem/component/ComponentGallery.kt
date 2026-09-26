package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El catalogo del sistema de diseno en una pantalla. No es una pantalla de la aplicacion:
 * existe para poder mirar todos los componentes juntos y para que la captura de ejemplo
 * tenga algo que capturar.
 *
 * Los textos van en duro a proposito: no es una pantalla del producto, no se traduce.
 */
@Composable
internal fun ComponentGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(GastonomoTheme.dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGap),
    ) {
        GastonomoLogo(wordmark = "gastonomo")
        GalleryButtons()
        GallerySegments()
        GalleryRows()
        GalleryControls()
    }
}

@Composable
private fun GalleryButtons() {
    PrimaryButton(text = "Guardar movimiento", onClick = {})
    SecondaryButton(text = "Continuar con Google", onClick = {})
    PrimaryButton(text = "Deshabilitado", onClick = {}, enabled = false)
}

@Composable
private fun GallerySegments() {
    SegmentedControl(
        segments = listOf(
            Segment("Gasto", GastonomoIcons.NORTH_EAST, GastonomoTheme.colors.red),
            Segment("Ingreso", GastonomoIcons.SOUTH_WEST, GastonomoTheme.colors.green),
        ),
        selectedIndex = 0,
        onSelect = {},
    )
    FilterPillRow {
        FilterPill("Todos", selected = true, onClick = {})
        FilterPill("Ingresos", selected = false, onClick = {})
        FilterPill("Gastos", selected = false, onClick = {})
    }
}

@Composable
private fun GalleryRows() {
    GastonomoCard(contentPadding = PaddingValues(vertical = ROW_GAP)) {
        ListRow(
            title = "Mercado",
            subtitle = "Alimentación · 15 sept",
            leading = {
                IconTile(background = tintOfCategoryColor(GastonomoTheme.colors.red)) {
                    GastonomoIcon(GastonomoIcons.RESTAURANT, tint = GastonomoTheme.colors.red)
                }
            },
            trailing = {
                GastonomoAmountText(
                    text = "−$ 186.400",
                    style = GastonomoTheme.typography.amount,
                    color = GastonomoTheme.colors.ink,
                )
            },
        )
        ListRow(
            title = "Salario",
            subtitle = "Salario · 1 sept",
            showDivider = false,
            leading = {
                IconTile(background = tintOfCategoryColor(GastonomoTheme.colors.green)) {
                    GastonomoIcon(
                        GastonomoIcons.ACCOUNT_BALANCE_WALLET,
                        tint = GastonomoTheme.colors.green,
                    )
                }
            },
            trailing = {
                GastonomoAmountText(
                    text = "+$ 2.800.000",
                    style = GastonomoTheme.typography.amount,
                    color = GastonomoTheme.colors.green,
                )
            },
        )
    }
}

@Composable
private fun GalleryControls() {
    GastonomoTextField(
        label = "Correo",
        value = "camilo@correo.com",
        onValueChange = {},
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGap),
    ) {
        GastonomoCheckbox(checked = true, onCheckedChange = {})
        GastonomoToggle(checked = true, onCheckedChange = {})
        GastonomoRadio(selected = true)
        StatusBadge(
            label = "Parcial",
            background = GastonomoTheme.colors.partialBg,
            contentColor = GastonomoTheme.colors.noteText,
        )
    }
    ProgressBar(fraction = 0.67f, fill = GastonomoTheme.colors.ink)
    EmptyState(icon = GastonomoIcons.RECEIPT_LONG, message = "No hay movimientos en este periodo.")
}

private val ROW_GAP = 4.dp
