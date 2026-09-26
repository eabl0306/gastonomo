package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.LocalContentColor
import com.bahoque.gastonomo.core.designsystem.theme.LocalTextStyle

/**
 * El boton principal: fondo ink, texto blanco, radio 16 y alto 56.
 *
 * Deshabilitado no se apaga ni se bloquea: baja a opacidad 0.4 y sigue siendo pulsable,
 * pero no hace nada. Es lo que pide el diseno, para que el usuario que lo toca entienda
 * que falta algo en el formulario en vez de encontrarse un control muerto.
 *
 * ```
 * PrimaryButton(text = stringResource(R.string.save), enabled = form.isValid, onClick = ::save)
 * PrimaryButton(text = ..., icon = GastonomoIcons.ADD, iconTint = GastonomoTheme.colors.lime) { }
 * ```
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: String? = null,
    iconTint: Color = GastonomoTheme.colors.lime,
) {
    ButtonSurface(
        modifier = modifier.height(GastonomoTheme.dimens.primaryButton),
        background = GastonomoTheme.colors.ink,
        border = null,
        enabled = enabled,
        onClick = onClick,
    ) {
        ButtonContent(text, icon, iconTint, GastonomoTheme.colors.surface, GastonomoTheme.typography.body)
    }
}

/** El boton secundario: fondo blanco, borde de input, alto 54. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: String? = null,
    iconTint: Color = GastonomoTheme.colors.ink,
    background: Color = GastonomoTheme.colors.surface,
    contentColor: Color = GastonomoTheme.colors.ink,
    border: Color? = GastonomoTheme.colors.inputBorder,
) {
    ButtonSurface(
        modifier = modifier.height(GastonomoTheme.dimens.secondaryButton),
        background = background,
        border = border,
        enabled = enabled,
        onClick = onClick,
    ) {
        ButtonContent(text, icon, iconTint, contentColor, GastonomoTheme.typography.body)
    }
}

/**
 * Un enlace de texto subrayado, como "Olvidaste tu contrasena" o "Usar sugerida".
 *
 * @param style el rol de texto del enlace. El de la caja de conversion es mas pequeno y mas
 *   grueso que el de una pantalla de autenticacion, asi que se puede cambiar.
 */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GastonomoTheme.colors.ink,
    style: TextStyle = GastonomoTheme.typography.secondary,
) {
    GastonomoText(
        text = text,
        modifier = modifier.pressable(onClick = onClick, role = Role.Button),
        style = style.copy(textDecoration = TextDecoration.Underline),
        color = color,
    )
}

@Composable
private fun ButtonSurface(
    background: Color,
    border: Color?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.control)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(GastonomoTheme.dimens.divider, border, shape) else Modifier)
            .pressable(onClick = { if (enabled) onClick() }, role = Role.Button),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    icon: String?,
    iconTint: Color,
    contentColor: Color,
    style: TextStyle,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides style,
    ) {
        if (icon != null) {
            GastonomoIcon(icon, size = ICON_SIZE, tint = iconTint)
            Spacer(Modifier.width(GAP))
        }
        GastonomoText(text, maxLines = 1)
    }
}

private val ICON_SIZE = 20.dp
private val GAP = 8.dp
