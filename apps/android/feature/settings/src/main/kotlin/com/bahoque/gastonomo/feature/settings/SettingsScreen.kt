package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.Avatar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.SecondaryButton
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.user.User

/**
 * La pantalla 12: la tarjeta de perfil, los tres grupos de ajustes, el boton de cerrar
 * sesion y el pie con la version.
 *
 * Es solo pintura: el estado y las acciones llegan de fuera, asi que se puede capturar y
 * probar sin ViewModel.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    version: String,
    modifier: Modifier = Modifier,
) {
    GastonomoScreen(modifier = modifier) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = BOTTOM_PADDING),
            verticalArrangement = Arrangement.spacedBy(GROUP_GAP),
        ) {
            GastonomoText(
                text = stringResource(R.string.settings_title),
                style = GastonomoTheme.typography.screenTitle,
                color = GastonomoTheme.colors.ink,
                modifier = Modifier.padding(top = TITLE_TOP, bottom = TITLE_BOTTOM),
            )
            ProfileCard(state.user)
            AccountGroup(state, actions)
            GeneralGroup(state, actions)
            DataGroup(state, actions)
            SecondaryButton(
                text = stringResource(R.string.settings_sign_out),
                onClick = actions.onSignOut,
                icon = GastonomoIcons.LOGOUT,
                iconTint = GastonomoTheme.colors.red,
                contentColor = GastonomoTheme.colors.red,
                border = GastonomoTheme.colors.line,
                modifier = Modifier.padding(top = SIGN_OUT_GAP),
            )
            GastonomoText(
                text = stringResource(GastonomoStrings.footer, version),
                style = GastonomoTheme.typography.secondarySmall.copy(textAlign = TextAlign.Center),
                color = GastonomoTheme.colors.faint,
                modifier = Modifier.padding(top = FOOTER_GAP),
            )
        }
    }
}

/**
 * La tarjeta de perfil. El diseno le pone un chevron, pero la pantalla de perfil no esta
 * disenada, asi que la tarjeta no es pulsable: un control que no lleva a ningun sitio
 * miente mas que un chevron quieto.
 */
@Composable
private fun ProfileCard(user: User?, modifier: Modifier = Modifier) {
    GastonomoCard(
        modifier = modifier,
        background = GastonomoTheme.colors.ink,
        border = null,
        radius = GastonomoTheme.radii.profileCard,
        contentColor = GastonomoTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
        ) {
            Avatar(
                initials = user?.initials.orEmpty(),
                size = GastonomoTheme.dimens.avatarLarge,
                background = GastonomoTheme.colors.lime,
                contentColor = GastonomoTheme.colors.ink,
            )
            Column(modifier = Modifier.weight(1f)) {
                GastonomoText(
                    text = user?.name.orEmpty(),
                    style = GastonomoTheme.typography.appBarTitle,
                    color = GastonomoTheme.colors.surface,
                    maxLines = 1,
                )
                GastonomoText(
                    text = user?.email.orEmpty(),
                    style = GastonomoTheme.typography.secondary,
                    color = GastonomoTheme.colors.balance.label,
                    maxLines = 1,
                )
            }
            GastonomoIcon(
                name = GastonomoIcons.CHEVRON_RIGHT,
                size = CHEVRON_SIZE,
                tint = GastonomoTheme.colors.balance.label,
            )
        }
    }
}

private val TITLE_TOP = 4.dp
private val TITLE_BOTTOM = 4.dp
private val GROUP_GAP = 12.dp
private val CARD_PADDING = 18.dp
private val CARD_GAP = 14.dp
private val CHEVRON_SIZE = 22.dp
private val SIGN_OUT_GAP = 12.dp
private val FOOTER_GAP = 4.dp
private val BOTTOM_PADDING = 28.dp
