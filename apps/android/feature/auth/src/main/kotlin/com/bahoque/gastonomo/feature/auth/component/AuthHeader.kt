package com.bahoque.gastonomo.feature.auth.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El H1 y el subtitulo que abren las tres pantallas con formulario. Login usa el estilo de
 * 32 y Registro y Recuperar el de 30, que es la unica diferencia entre ellas.
 */
@Composable
internal fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = GastonomoTheme.typography.authTitleSmall,
    titleTopPadding: Dp = TITLE_TOP_PADDING,
    subtitleBottomPadding: Dp = SUBTITLE_BOTTOM_PADDING,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GastonomoText(
            text = title,
            style = titleStyle,
            color = GastonomoTheme.colors.ink,
            modifier = Modifier.padding(top = titleTopPadding, bottom = TITLE_BOTTOM_PADDING),
        )
        GastonomoText(
            text = subtitle,
            style = GastonomoTheme.typography.body.copy(lineHeight = SUBTITLE_LINE_HEIGHT),
            color = GastonomoTheme.colors.muted,
            modifier = Modifier.padding(bottom = subtitleBottomPadding),
        )
    }
}

/**
 * El pie de Login y de Registro: una pregunta en muted y la accion en ink y negrita.
 *
 * Solo la accion es pulsable, como en el prototipo, y lleva relleno vertical para llegar
 * al minimo tactil de 44 dp.
 */
@Composable
internal fun AuthFooterPrompt(
    question: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoText(
            text = question,
            style = GastonomoTheme.typography.bodySmall,
            color = GastonomoTheme.colors.muted,
        )
        GastonomoText(
            text = action,
            style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = GastonomoTheme.colors.ink,
            modifier = Modifier
                .heightIn(min = GastonomoTheme.dimens.minTouchTarget)
                .pressable(onClick = onClick, role = Role.Button)
                .padding(start = ACTION_GAP, top = ACTION_PADDING, bottom = ACTION_PADDING),
        )
    }
}

/** El `margin:20px 0 8px` del H1 de Registro y de Recuperar. */
private val TITLE_TOP_PADDING = 20.dp
private val TITLE_BOTTOM_PADDING = 8.dp

/** El `margin-bottom:28px` del subtitulo de Registro y de Recuperar. */
private val SUBTITLE_BOTTOM_PADDING = 28.dp

/** El `line-height:1.5` de los subtitulos de 15 px. */
private val SUBTITLE_LINE_HEIGHT = 22.5.sp

private val ACTION_GAP = 4.dp
private val ACTION_PADDING = 12.dp
