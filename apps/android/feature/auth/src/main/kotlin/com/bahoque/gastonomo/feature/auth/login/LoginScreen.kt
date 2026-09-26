package com.bahoque.gastonomo.feature.auth.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoLogo
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.component.TextLink
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.component.AuthDivider
import com.bahoque.gastonomo.feature.auth.component.AuthFooterPrompt
import com.bahoque.gastonomo.feature.auth.component.AuthFormError
import com.bahoque.gastonomo.feature.auth.component.AuthHeader
import com.bahoque.gastonomo.feature.auth.component.AuthScaffold
import com.bahoque.gastonomo.feature.auth.component.LOGIN_TOP_PADDING
import com.bahoque.gastonomo.feature.auth.text

/**
 * Pantalla 1 del rediseno: entrar con correo y contrasena, o con el cerrojo local.
 *
 * El boton de Google del prototipo **no se pinta**: el usuario decidio quitarlo y la API
 * no tiene login con Google.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onUnlockWithLocalLock: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        modifier = modifier,
        topPadding = LOGIN_TOP_PADDING,
        footer = {
            AuthFooterPrompt(
                question = stringResource(R.string.auth_login_no_account),
                action = stringResource(R.string.auth_login_register),
                onClick = onRegister,
            )
        },
    ) {
        GastonomoLogo(wordmark = stringResource(GastonomoStrings.wordmark))
        AuthHeader(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle),
            titleStyle = GastonomoTheme.typography.authTitle,
            titleTopPadding = TITLE_TOP_PADDING,
            subtitleBottomPadding = SUBTITLE_BOTTOM_PADDING,
        )
        Credentials(
            state = state,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
        )
        ForgotPasswordLink(onForgotPassword)
        Spacer(Modifier.height(BUTTON_GAP))
        state.formError?.let { AuthFormError(message = it.text()) }
        PrimaryButton(
            text = stringResource(R.string.auth_login_submit),
            onClick = onSubmit,
            enabled = !state.submitting,
        )
        AuthDivider(
            label = stringResource(R.string.auth_divider_or),
            modifier = Modifier.padding(vertical = DIVIDER_PADDING),
        )
        SecondaryButton(
            text = stringResource(R.string.auth_login_biometrics),
            onClick = onUnlockWithLocalLock,
            icon = GastonomoIcons.FINGERPRINT,
            iconTint = GastonomoTheme.colors.ink,
            background = GastonomoTheme.colors.chipBg,
            border = null,
        )
        Spacer(Modifier.height(FOOTER_GAP))
    }
}

@Composable
private fun ColumnScope.Credentials(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
    GastonomoTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.auth_field_email),
        placeholder = stringResource(R.string.auth_field_email_placeholder),
        error = state.emailError?.text(),
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
    )
    Spacer(Modifier.height(FIELD_GAP))
    PasswordField(
        value = state.password,
        onValueChange = onPasswordChange,
        visible = state.passwordVisible,
        onToggleVisibility = onTogglePasswordVisibility,
        error = state.passwordError?.text(),
    )
}

/** El enlace va pegado a la derecha y subrayado, con el alto tactil del diseno. */
@Composable
private fun ForgotPasswordLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        TextLink(
            text = stringResource(R.string.auth_login_forgot),
            onClick = onClick,
            style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(vertical = LINK_PADDING),
        )
    }
}

/** El `margin:48px 0 8px` del H1 de Login. */
private val TITLE_TOP_PADDING = 48.dp
private val SUBTITLE_BOTTOM_PADDING = 32.dp
private val FIELD_GAP = 18.dp
private val LINK_PADDING = 12.dp

/** El `margin-top:8px` del boton de entrar. */
private val BUTTON_GAP = 8.dp
private val DIVIDER_PADDING = 22.dp
private val FOOTER_GAP = 24.dp
