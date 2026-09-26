package com.bahoque.gastonomo.feature.auth.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R

/**
 * El campo de contrasena. Lo usan Login y Registro, y por eso vive suelto en vez de dentro de
 * una de las dos pantallas.
 *
 * El ojo `visibility` / `visibility_off` sale solo cuando hay un [onToggleVisibility]: el
 * diseno lo dibuja en Login y no en Registro.
 */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    placeholder: String? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    GastonomoTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = stringResource(R.string.auth_field_password),
        placeholder = placeholder ?: stringResource(R.string.auth_field_password_placeholder),
        error = error,
        keyboardType = if (visible) KeyboardType.Text else KeyboardType.Password,
        imeAction = imeAction,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailing = onToggleVisibility?.let { toggle ->
            {
                VisibilityToggle(visible = visible, onToggle = toggle)
            }
        },
    )
}

@Composable
private fun VisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(GastonomoTheme.dimens.minTouchTarget)
            .pressable(onClick = onToggle, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = if (visible) GastonomoIcons.VISIBILITY_OFF else GastonomoIcons.VISIBILITY,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.muted,
            contentDescription = stringResource(
                if (visible) R.string.auth_hide_password else R.string.auth_show_password,
            ),
        )
    }
}

private val ICON_SIZE = 22.dp
