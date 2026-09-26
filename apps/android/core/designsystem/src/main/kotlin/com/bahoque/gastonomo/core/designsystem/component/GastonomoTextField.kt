package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El campo de texto del diseno: label encima, alto 54, radio 16, borde `inputBorder` que
 * pasa a ink al recibir el foco.
 *
 * [height], [radius] y [leading] existen para el buscador del sheet de moneda, que el
 * diseno pinta mas bajo, menos redondeado y con la lupa delante. Todo lo demas usa los
 * valores por defecto.
 *
 * El label va en ink, que es lo que hace el prototipo en los seis campos de 13 px. Los
 * unicos labels en muted del diseno son los de 12 px de los sheets de rango y de abono, y
 * para esos se pasa [labelColor]. Con [optional], detras del label sale "(opcional)" en
 * faint y peso 500, tal como lo pinta el prototipo en las dos notas.
 *
 * El error se pinta debajo en rojo y es el `errors[].message` que manda la API, que ya
 * viene en castellano listo para ensenar: no hay que traducirlo.
 *
 * ```
 * GastonomoTextField(
 *     label = stringResource(R.string.email),
 *     value = state.email,
 *     onValueChange = viewModel::onEmail,
 *     keyboardType = KeyboardType.Email,
 *     error = state.emailError,
 * )
 * ```
 */
@Composable
fun GastonomoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    labelColor: Color = GastonomoTheme.colors.ink,
    optional: Boolean = false,
    height: Dp = GastonomoTheme.dimens.textField,
    radius: Dp = GastonomoTheme.radii.control,
    contentPadding: Dp = FIELD_PADDING,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            GastonomoFieldLabel(
                label = label,
                color = labelColor,
                optional = optional,
                modifier = Modifier.padding(bottom = LABEL_GAP),
            )
        }
        FieldBox(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            enabled = enabled,
            hasError = error != null,
            keyboardType = keyboardType,
            imeAction = imeAction,
            visualTransformation = visualTransformation,
            height = height,
            radius = radius,
            contentPadding = contentPadding,
            leading = leading,
            trailing = trailing,
        )
        if (error != null) {
            GastonomoText(
                text = error,
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.red,
                modifier = Modifier.padding(top = LABEL_GAP),
            )
        }
    }
}

/**
 * El label de un campo: 13 px en semibold, con "(opcional)" detras en faint y peso 500
 * cuando el campo se puede dejar vacio.
 *
 * Esta suelto porque [GastonomoDateField] pinta el suyo igual y no comparte caja con este.
 */
@Composable
fun GastonomoFieldLabel(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = GastonomoTheme.colors.ink,
    style: TextStyle = GastonomoTheme.typography.fieldLabel,
    optional: Boolean = false,
) {
    if (!optional) {
        GastonomoText(text = label, style = style, color = color, modifier = modifier)
        return
    }
    val faint = GastonomoTheme.colors.faint
    val suffix = stringResource(GastonomoStrings.fieldOptional)
    val text = remember(label, suffix, faint) {
        buildAnnotatedString {
            append(label)
            append(" ")
            withStyle(SpanStyle(color = faint, fontWeight = FontWeight.Medium)) { append(suffix) }
        }
    }
    GastonomoText(text = text, style = style, color = color, modifier = modifier)
}

@Composable
private fun FieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    enabled: Boolean,
    hasError: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    visualTransformation: VisualTransformation,
    height: Dp,
    radius: Dp,
    contentPadding: Dp,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(radius)
    val borderColor = when {
        hasError -> GastonomoTheme.colors.red
        focused -> GastonomoTheme.colors.ink
        else -> GastonomoTheme.colors.inputBorder
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .background(GastonomoTheme.colors.surface, shape)
            .border(GastonomoTheme.dimens.divider, borderColor, shape)
            .padding(horizontal = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        leading?.invoke()
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty() && placeholder != null) {
                GastonomoText(
                    text = placeholder,
                    style = GastonomoTheme.typography.body,
                    color = GastonomoTheme.colors.faint,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = GastonomoTheme.typography.body.copy(color = GastonomoTheme.colors.ink),
                cursorBrush = SolidColor(GastonomoTheme.colors.ink),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

/** El diseno separa el label del campo 8 px. */
private val LABEL_GAP = 8.dp

/** Los inputs del diseno llevan 16 de padding. El buscador de moneda pasa 14. */
val FIELD_PADDING = 16.dp
private val GAP = 8.dp
