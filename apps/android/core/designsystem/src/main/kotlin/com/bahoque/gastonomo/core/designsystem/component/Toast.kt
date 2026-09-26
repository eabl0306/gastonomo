package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import kotlinx.coroutines.delay

/**
 * El toast del diseno: abajo, sobre ink, con `check_circle` en lime, y se oculta a los
 * 2,2 segundos.
 *
 * Se pinta encima de la pantalla y quien lo ensena solo tiene que guardar un mensaje en
 * el estado y limpiarlo cuando [onHide] avisa. Hay un unico toast a la vez, igual que en
 * el prototipo.
 *
 * [icon] e [iconTint] existen para lo que el diseno no cubre: contar un fallo. Un aviso de
 * error con el check en lime miente, asi que se cambia el icono y se deja el resto igual.
 *
 * ```
 * state.toast?.let { GastonomoToast(message = it, onHide = viewModel::clearToast) }
 * ```
 */
@Composable
fun GastonomoToast(
    message: String,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String = GastonomoIcons.CHECK_CIRCLE,
    iconTint: Color = GastonomoTheme.colors.lime,
) {
    LaunchedEffect(message) {
        delay(VISIBLE_MILLIS)
        onHide()
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SIDE_MARGIN)
                .padding(bottom = BOTTOM_MARGIN)
                .shadow(GastonomoTheme.shadows.toast, RoundedCornerShape(GastonomoTheme.radii.control))
                .background(GastonomoTheme.colors.ink, RoundedCornerShape(GastonomoTheme.radii.control))
                .padding(horizontal = CONTENT_PADDING, vertical = CONTENT_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            GastonomoIcon(icon, size = ICON_SIZE, tint = iconTint)
            GastonomoText(
                text = message,
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.surface,
            )
        }
    }
}

/** Lo que el diseno fija: 2,2 segundos visible. */
const val VISIBLE_MILLIS = 2_200L

private val SIDE_MARGIN = 20.dp

/**
 * El diseno pone el toast a 96 del borde inferior del telefono, contando los 78 de la tab
 * bar. Aqui la tab bar es hermana del contenido y no lo tapa, asi que la distancia es la que
 * queda: 96 menos 78.
 */
private val BOTTOM_MARGIN = 18.dp

private val CONTENT_PADDING = 14.dp
private val ICON_SIZE = 20.dp
private val GAP = 10.dp
