package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.SheetHeader

/**
 * La hoja que pide el PIN al encender el cerrojo local.
 *
 * El diseno no tiene pantalla para crear el PIN: solo tiene la de desbloqueo, que ya da por
 * hecho que existe uno. Sin esta hoja el toggle de Ajustes no se puede encender, asi que se
 * anade lo minimo: cuatro digitos y un boton.
 */
@Composable
internal fun PinSheet(
    pin: String,
    onPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GastonomoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.settings_pin_title),
            subtitle = stringResource(R.string.settings_pin_subtitle),
        )
        GastonomoTextField(
            value = pin,
            onValueChange = onPinChanged,
            label = stringResource(R.string.settings_pin_label),
            placeholder = stringResource(R.string.settings_pin_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            visualTransformation = PasswordVisualTransformation(),
        )
        PrimaryButton(
            text = stringResource(R.string.settings_pin_save),
            onClick = onConfirm,
            enabled = pin.length == LocalLock.PIN_LENGTH,
            modifier = Modifier.padding(top = BUTTON_GAP),
        )
    }
}

private val BUTTON_GAP = 20.dp
