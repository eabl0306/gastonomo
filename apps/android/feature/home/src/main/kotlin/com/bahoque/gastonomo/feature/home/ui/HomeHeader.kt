package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.bahoque.gastonomo.core.designsystem.component.Avatar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.feature.home.R
import java.time.LocalDate

/** "Hola, Camilo" con el perfil, y "Hola" a secas mientras no ha llegado. */
@Composable
private fun greetingOf(user: User?): String {
    val name = user?.firstName.orEmpty()
    if (name.isEmpty()) return stringResource(R.string.home_greeting_anonymous)
    return stringResource(R.string.home_greeting, name)
}

/**
 * El encabezado de Inicio: la fecha de hoy, el saludo y el avatar que lleva a Ajustes.
 *
 * Con el perfil todavia sin llegar se ensena el saludo sin nombre y el avatar vacio, en vez
 * de un hueco: el resto de la pantalla ya tiene datos que ensenar.
 */
@Composable
internal fun HomeHeader(
    today: LocalDate,
    user: User?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            GastonomoText(
                text = GastonomoTheme.formats.dates.weekdayWithDate(today),
                style = GastonomoTheme.typography.secondary,
                color = GastonomoTheme.colors.muted,
            )
            GastonomoText(
                text = greetingOf(user),
                style = GastonomoTheme.typography.greeting,
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
        }
        Avatar(
            initials = user?.initials.orEmpty(),
            modifier = Modifier.pressable(
                onClick = onOpenSettings,
                role = Role.Button,
            ),
        )
    }
}
