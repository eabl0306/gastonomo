package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoFontFamily
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El avatar circular con iniciales: ink con letras lime en el header y en el PIN, lime con
 * letras ink en la tarjeta de perfil, azul o ambar en un prestamo.
 *
 * Las iniciales las da el dominio
 * ([com.bahoque.gastonomo.core.domain.user.User.initials]); aqui solo se pintan.
 *
 * El diseno escala las iniciales con el circulo: 15 en los de 40 y 44, 18 en los de 48 y
 * 52, y 22 en el de 64. Por eso [textStyle] es un parametro y no un valor fijo.
 *
 * ```
 * Avatar(initials = user.initials, size = GastonomoTheme.dimens.avatar)
 * Avatar(initials = "R", background = colors.blueBg, contentColor = colors.blue, size = 40.dp)
 * Avatar(
 *     initials = user.initials,
 *     size = GastonomoTheme.dimens.avatarHuge,
 *     textStyle = GastonomoTheme.typography.avatarInitialsHuge,
 * )
 * ```
 */
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = GastonomoTheme.dimens.avatar,
    background: Color = GastonomoTheme.colors.ink,
    contentColor: Color = GastonomoTheme.colors.lime,
    textStyle: TextStyle = GastonomoTheme.typography.avatarInitials,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = initials,
            style = textStyle,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * El logo tipografico: un cuadrado ink de 36 con una "g" en lime y la palabra
 * "gastonomo" al lado. No hay imagen en el diseno.
 */
@Composable
fun GastonomoLogo(
    wordmark: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        Box(
            modifier = Modifier
                .size(MARK_SIZE)
                .background(GastonomoTheme.colors.ink, RoundedCornerShape(GastonomoTheme.radii.logo)),
            contentAlignment = Alignment.Center,
        ) {
            GastonomoText(
                text = MARK_LETTER,
                style = GastonomoTheme.typography.logoMark,
                color = GastonomoTheme.colors.lime,
            )
        }
        GastonomoText(
            text = wordmark,
            style = GastonomoTheme.typography.logoWord,
            color = GastonomoTheme.colors.ink,
        )
    }
}

/** El estilo de un monto grande con el simbolo mas pequeno delante, como en los formularios. */
fun symbolStyleFor(amountStyle: TextStyle, symbolSize: Int): TextStyle =
    amountStyle.copy(fontSize = symbolSize.sp, fontFamily = GastonomoFontFamily)

private const val MARK_LETTER = "g"
private val MARK_SIZE = 36.dp
private val GAP = 10.dp
