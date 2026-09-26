package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.component.AuthFooterPrompt
import com.bahoque.gastonomo.feature.auth.component.AuthFormError
import com.bahoque.gastonomo.feature.auth.component.AuthHeader
import com.bahoque.gastonomo.feature.auth.component.AuthScaffold
import com.bahoque.gastonomo.feature.auth.login.PasswordField
import com.bahoque.gastonomo.feature.auth.text

/**
 * Pantalla 2 del rediseno: crear la cuenta.
 *
 * El boton de Google del prototipo **no se pinta**, igual que en Login: el usuario lo quito
 * y la API no tiene registro con Google.
 */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    callbacks: RegisterCallbacks,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        modifier = modifier,
        onBack = callbacks.onBack,
        footer = {
            AuthFooterPrompt(
                question = stringResource(R.string.auth_register_have_account),
                action = stringResource(R.string.auth_register_login),
                onClick = callbacks.onSignIn,
            )
        },
    ) {
        AuthHeader(
            title = stringResource(R.string.auth_register_title),
            subtitle = stringResource(R.string.auth_register_subtitle),
        )
        Fields(state = state, callbacks = callbacks)
        PasswordStrengthMeter(strength = state.strength)
        Spacer(Modifier.height(TERMS_GAP))
        TermsCheckbox(
            checked = state.acceptedTerms,
            onToggle = callbacks.onToggleTerms,
            error = state.termsError,
        )
        Spacer(Modifier.height(BUTTON_GAP))
        state.formError?.let { AuthFormError(message = it.text()) }
        PrimaryButton(
            text = stringResource(R.string.auth_register_submit),
            onClick = callbacks.onSubmit,
            enabled = !state.submitting,
        )
        Spacer(Modifier.height(FOOTER_GAP))
    }
}

@Composable
private fun ColumnScope.Fields(state: RegisterUiState, callbacks: RegisterCallbacks) {
    GastonomoTextField(
        value = state.name,
        onValueChange = callbacks.onNameChange,
        label = stringResource(R.string.auth_field_name),
        placeholder = stringResource(R.string.auth_field_name_placeholder),
        error = state.nameError?.text(),
        imeAction = ImeAction.Next,
    )
    Spacer(Modifier.height(FIELD_GAP))
    GastonomoTextField(
        value = state.email,
        onValueChange = callbacks.onEmailChange,
        label = stringResource(R.string.auth_field_email),
        placeholder = stringResource(R.string.auth_field_email_placeholder),
        error = state.emailError?.text(),
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
    )
    Spacer(Modifier.height(FIELD_GAP))
    // El diseno no dibuja el ojo en Registro, solo en Login: aqui no se pasa el toggle.
    PasswordField(
        value = state.password,
        onValueChange = callbacks.onPasswordChange,
        placeholder = stringResource(R.string.auth_register_password_placeholder),
        error = state.passwordError?.text(),
    )
}

private val FIELD_GAP = 18.dp

/**
 * El diseno deja 20 dp entre el medidor y los terminos y 24 entre los terminos y el boton.
 * La fila de los terminos ya aporta 11 dp arriba y abajo para llegar al minimo tactil de
 * 44, asi que aqui van los que faltan.
 */
private val TERMS_GAP = 9.dp
private val BUTTON_GAP = 13.dp
private val FOOTER_GAP = 20.dp
