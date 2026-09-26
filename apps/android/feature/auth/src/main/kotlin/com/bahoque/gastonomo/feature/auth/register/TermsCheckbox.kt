package com.bahoque.gastonomo.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCheckbox
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.auth.AuthMessage
import com.bahoque.gastonomo.feature.auth.R
import com.bahoque.gastonomo.feature.auth.text

/**
 * El checkbox de 22 con radio 7 y la frase de los terminos.
 *
 * En el prototipo toda la fila es pulsable, asi que el texto tambien marca la casilla, y
 * el lector de pantalla la anuncia como una sola casilla con ese texto.
 */
@Composable
internal fun TermsCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    error: AuthMessage? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onClick = onToggle, role = Role.Checkbox)
                .padding(vertical = ROW_PADDING),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            GastonomoCheckbox(checked = checked, onCheckedChange = { onToggle() })
            GastonomoText(
                text = stringResource(R.string.auth_register_terms),
                style = GastonomoTheme.typography.secondary.copy(lineHeight = LINE_HEIGHT),
                color = GastonomoTheme.colors.muted,
            )
        }
        error?.let {
            GastonomoText(
                text = it.text(),
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.red,
            )
        }
    }
}

/** El `line-height:1.45` sobre 13 px. */
private val LINE_HEIGHT = 18.85.sp
private val GAP = 10.dp

/** Lo que le falta a la fila para llegar al minimo tactil de 44 dp. */
private val ROW_PADDING = 11.dp
