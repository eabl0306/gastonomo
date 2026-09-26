package com.bahoque.gastonomo.feature.auth.forgot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.component.AuthFormError
import com.bahoque.gastonomo.feature.auth.component.AuthHeader
import com.bahoque.gastonomo.feature.auth.component.AuthScaffold
import com.bahoque.gastonomo.feature.auth.text

/**
 * Pantalla 3 del rediseno, con sus dos estados: el formulario y el "Revisa tu correo".
 *
 * La pantalla nunca dice si la cuenta existe. La API devuelve 202 en los dos casos y aqui
 * se pasa al segundo estado igual, porque un mensaje distinto seria un oraculo para saber
 * quien tiene cuenta.
 */
@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onBackToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(modifier = modifier, onBack = onBack) {
        if (state.sent) {
            SentState(state = state, onBackToLogin = onBackToLogin, onResend = onResend)
        } else {
            FormState(state = state, onEmailChange = onEmailChange, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun ColumnScope.FormState(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    AuthHeader(
        title = stringResource(R.string.auth_forgot_title),
        subtitle = stringResource(R.string.auth_forgot_subtitle),
    )
    GastonomoTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.auth_field_email),
        placeholder = stringResource(R.string.auth_field_email_placeholder),
        error = state.emailError?.text(),
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
    )
    Spacer(Modifier.height(SUBMIT_GAP))
    state.formError?.let { AuthFormError(message = it.text()) }
    PrimaryButton(
        text = stringResource(R.string.auth_forgot_submit),
        onClick = onSubmit,
        enabled = !state.submitting,
    )
}

@Composable
private fun ColumnScope.SentState(
    state: ForgotPasswordUiState,
    onBackToLogin: () -> Unit,
    onResend: () -> Unit,
) {
    MailTile()
    GastonomoText(
        text = stringResource(R.string.auth_forgot_sent_title),
        style = GastonomoTheme.typography.authTitleSmall,
        color = GastonomoTheme.colors.ink,
        modifier = Modifier.padding(top = TILE_GAP, bottom = TITLE_BOTTOM_PADDING),
    )
    GastonomoText(
        text = sentBody(state.sentTo),
        style = GastonomoTheme.typography.body.copy(lineHeight = BODY_LINE_HEIGHT),
        color = GastonomoTheme.colors.muted,
        modifier = Modifier.padding(bottom = BODY_BOTTOM_PADDING),
    )
    state.formError?.let { AuthFormError(message = it.text()) }
    PrimaryButton(text = stringResource(R.string.auth_forgot_back_to_login), onClick = onBackToLogin)
    Spacer(Modifier.height(RESEND_GAP))
    ResendLink(onResend = onResend, enabled = !state.submitting)
}

/** "No llego? Reenviar": boton de texto de 48, sin fondo ni borde, en muted. */
@Composable
private fun ResendLink(onResend: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RESEND_HEIGHT)
            .pressable(onClick = onResend, enabled = enabled, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = stringResource(R.string.auth_forgot_resend),
            style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = GastonomoTheme.colors.muted,
        )
    }
}

/** "Enviamos un enlace a **{correo}**. Caduca en 30 minutos.", con el correo resaltado. */
@Composable
private fun sentBody(email: String): AnnotatedString {
    val shown = email.ifBlank { stringResource(R.string.auth_forgot_email_fallback) }
    val body = stringResource(R.string.auth_forgot_sent_body, shown)
    val highlight = SpanStyle(color = GastonomoTheme.colors.ink, fontWeight = FontWeight.Bold)
    val start = body.indexOf(shown)
    if (start < 0) return AnnotatedString(body)
    return buildAnnotatedString {
        append(body.substring(0, start))
        withStyle(highlight) { append(shown) }
        append(body.substring(start + shown.length))
    }
}

/** El tile de 64 con radio 20 en lime y el `mark_email_read`. */
@Composable
private fun MailTile() {
    Box(
        modifier = Modifier
            .padding(top = TILE_TOP_PADDING)
            .size(TILE_SIZE)
            .background(GastonomoTheme.colors.lime, RoundedCornerShape(GastonomoTheme.radii.fab)),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            GastonomoIcons.MARK_EMAIL_READ,
            size = TILE_ICON_SIZE,
            tint = GastonomoTheme.colors.ink,
        )
    }
}

private val SUBMIT_GAP = 24.dp
private val TILE_SIZE = 64.dp
private val TILE_ICON_SIZE = 32.dp
private val TILE_TOP_PADDING = 40.dp
private val TILE_GAP = 24.dp
private val TITLE_BOTTOM_PADDING = 8.dp
private val BODY_BOTTOM_PADDING = 28.dp
private val BODY_LINE_HEIGHT = 22.5.sp
private val RESEND_GAP = 6.dp
private val RESEND_HEIGHT = 48.dp
