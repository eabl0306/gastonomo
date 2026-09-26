package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
 * La segunda mitad del catalogo: lo que va encima de una pantalla o la enmarca. Igual que
 * [ComponentGallery], no es una pantalla del producto y sus textos no se traducen.
 */
@Composable
internal fun OverlayGallery(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GastonomoAppBar(title = "Detalle del préstamo", onNavigate = {})
            GalleryChrome(modifier = Modifier.weight(1f))
            GastonomoTabBar(
                items = TABS,
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        GastonomoFab(
            text = "Nuevo",
            onClick = {},
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = FAB_END, bottom = FAB_BOTTOM),
        )
    }
}

/** El velo, la hoja y el toast, que se pintan encima de todo lo demas. */
@Composable
internal fun OverlaySheetGallery(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        GastonomoBottomSheet(onDismiss = {}) {
            SheetHeader(
                title = "Rango de fechas",
                subtitle = "Se aplica al inicio y a movimientos.",
            )
            RadioCard(
                title = "Excel",
                subtitle = "Un libro con una hoja por conjunto",
                selected = true,
                onClick = {},
                leading = {
                    IconTile(
                        background = GastonomoTheme.colors.greenBg,
                        radius = GastonomoTheme.radii.iconTileTiny,
                    ) {
                        GastonomoIcon(GastonomoIcons.TABLE_VIEW, tint = GastonomoTheme.colors.green)
                    }
                },
            )
            RadioCard(title = "CSV", selected = false, onClick = {})
        }
        GastonomoToast(message = "Gasto guardado", onHide = {})
    }
}

@Composable
private fun GalleryChrome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(GastonomoTheme.dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGap),
    ) {
        Avatar(initials = "CR", size = GastonomoTheme.dimens.avatarHuge)
        Avatar(
            initials = "R",
            background = GastonomoTheme.colors.blueBg,
            contentColor = GastonomoTheme.colors.blue,
        )
    }
}

private val TABS = listOf(
    TabItem(GastonomoIcons.HOME, "Inicio"),
    TabItem(GastonomoIcons.SWAP_VERT, "Movimientos"),
    TabItem(GastonomoIcons.HANDSHAKE, "Préstamos"),
    TabItem(GastonomoIcons.SETTINGS, "Ajustes"),
)

private val FAB_END = 20.dp
private val FAB_BOTTOM = 92.dp
