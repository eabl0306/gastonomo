package com.bahoque.gastonomo.feature.auth.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.Avatar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.text

/**
 * Pantalla 4 del rediseno: el cerrojo local. Avatar, cuatro puntos, teclado y el enlace
 * para entrar con la contrasena.
 *
 * No hay boton de vuelta: esta pantalla es la puerta, y salir de ella es entrar con la
 * contrasena.
 */
@Composable
fun UnlockScreen(
    state: UnlockUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometrics: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GastonomoTheme.colors.bgApp)
            .navigationBarsPadding()
            .padding(
                start = SCREEN_PADDING,
                end = SCREEN_PADDING,
                top = TOP_PADDING,
                bottom = BOTTOM_PADDING,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Greeting(state = state)
        PinDots(digits = state.digits, modifier = Modifier.padding(top = DOTS_TOP_PADDING))
        // El prototipo empuja el teclado abajo con un `margin-bottom:auto` en los puntos.
        Spacer(Modifier.weight(1f))
        PinKeypad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            onBiometrics = onBiometrics,
            modifier = Modifier.padding(top = KEYPAD_TOP_PADDING),
        )
        UsePasswordLink(onUsePassword)
    }
}

@Composable
private fun Greeting(state: UnlockUiState) {
    Avatar(
        initials = state.initials,
        modifier = Modifier.padding(top = AVATAR_TOP_PADDING),
        size = GastonomoTheme.dimens.avatarHuge,
        textStyle = GastonomoTheme.typography.avatarInitialsHuge,
    )
    GastonomoText(
        text = stringResource(R.string.auth_unlock_greeting, state.name),
        style = GastonomoTheme.typography.unlockGreeting,
        color = GastonomoTheme.colors.ink,
        modifier = Modifier.padding(top = GREETING_TOP_PADDING),
    )
    GastonomoText(
        text = state.message?.text() ?: stringResource(R.string.auth_unlock_prompt),
        style = GastonomoTheme.typography.body.copy(textAlign = TextAlign.Center),
        color = if (state.message != null) GastonomoTheme.colors.red else GastonomoTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PROMPT_TOP_PADDING),
    )
}

/** Los cuatro puntos de 14 con borde ink, que se rellenan al teclear. */
@Composable
private fun PinDots(digits: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.auth_unlock_dots, digits, digits)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
    ) {
        repeat(UnlockUiState.PIN_LENGTH) { index ->
            Box(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .background(
                        if (index < digits) GastonomoTheme.colors.ink else Color.Transparent,
                        CircleShape,
                    )
                    .border(DOT_BORDER, GastonomoTheme.colors.ink, CircleShape),
            )
        }
    }
}

@Composable
private fun UsePasswordLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, role = Role.Button)
            .padding(vertical = LINK_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = stringResource(R.string.auth_unlock_use_password),
            style = GastonomoTheme.typography.bodySmall,
            color = GastonomoTheme.colors.muted,
        )
    }
}

/** El `padding:24px 32px 28px` de la pantalla de desbloqueo. */
private val SCREEN_PADDING = 32.dp
private val TOP_PADDING = 24.dp
private val BOTTOM_PADDING = 28.dp
private val AVATAR_TOP_PADDING = 36.dp
private val GREETING_TOP_PADDING = 18.dp
private val PROMPT_TOP_PADDING = 6.dp
private val DOTS_TOP_PADDING = 36.dp
private val KEYPAD_TOP_PADDING = 32.dp
private val DOT_SIZE = 14.dp
private val DOT_GAP = 18.dp
private val DOT_BORDER = 1.5.dp
private val LINK_PADDING = 12.dp
