package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Los tres estados que el diseno repite en todas las pantallas que cargan algo: cargando,
 * fallo con reintento y el aviso en ambar cuando falta un dato pero la pantalla sirve igual.
 *
 * Estaban copiados en Inicio, Movimientos, Categorias y Exportar con los mismos valores. Una
 * copia por pantalla es exactamente como cuatro pantallas acaban ensenando lo mismo de cuatro
 * maneras distintas.
 */
@Composable
fun GastonomoLoadingBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = STATE_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = stringResource(GastonomoStrings.loading),
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.muted,
        )
    }
}

/** El fallo de una pantalla que ya tiene su marco pintado: el mensaje y "Reintentar". */
@Composable
fun GastonomoRetryBlock(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = STATE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GastonomoText(
            text = message,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.muted,
        )
        Spacer(Modifier.height(BLOCK_GAP))
        SecondaryButton(text = stringResource(GastonomoStrings.actionRetry), onClick = onRetry)
    }
}

/**
 * El fallo de una pantalla que no tiene nada que ensenar todavia: el estado vacio con su icono
 * y, debajo, "Reintentar". Es lo que usan Categorias y Exportar.
 */
@Composable
fun GastonomoEmptyStateRetry(
    icon: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        EmptyState(icon = icon, message = message)
        SecondaryButton(text = stringResource(GastonomoStrings.actionRetry), onClick = onRetry)
    }
}

/**
 * El aviso en ambar debajo del titulo: las tasas no estan listas, el catalogo esta vacio. No es
 * un error, asi que no lleva boton y la pantalla sigue siendo usable.
 */
@Composable
fun GastonomoNoteLine(text: String, modifier: Modifier = Modifier) {
    GastonomoText(
        text = text,
        modifier = modifier.padding(top = NOTE_GAP),
        style = GastonomoTheme.typography.secondarySmall,
        color = GastonomoTheme.colors.amber,
    )
}

private val STATE_PADDING = 40.dp
private val BLOCK_GAP = 14.dp
private val NOTE_GAP = 8.dp
