package com.bahoque.gastonomo.core.designsystem.component

import android.app.Activity
import android.app.DatePickerDialog
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import java.time.LocalDate
import java.time.ZoneId

/**
 * El campo de fecha de los formularios y del sheet de rango. El diseno lo dibuja como un
 * `<input type="date">`, asi que aqui es una caja con la fecha escrita y el selector nativo
 * del sistema detras: en un telefono nadie teclea una fecha a mano.
 *
 * ```
 * GastonomoDateField(
 *     label = stringResource(R.string.movement_date),
 *     value = state.date,
 *     onValueChange = viewModel::onDate,
 *     maxDate = state.today,
 * )
 * ```
 *
 * El selector se abre desde la **actividad**, no desde `LocalContext`. Un dialogo del sistema
 * necesita el token de ventana de una actividad, y `LocalContext` en esta aplicacion es un
 * contexto de configuracion (lo pone el tema para el idioma de Ajustes), que no lo tiene:
 * tocar el campo cerraba la aplicacion con `BadTokenException`.
 *
 * @param compact la variante del sheet de rango: 50 de alto, radio 14 y label mas pequeno.
 */
@Composable
fun GastonomoDateField(
    label: String,
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    compact: Boolean = false,
) {
    val activity = LocalActivity.current
    val look = lookOf(compact)
    Column(modifier = modifier.fillMaxWidth()) {
        GastonomoText(
            text = label,
            style = look.labelStyle,
            color = look.labelColor,
            modifier = Modifier.padding(bottom = look.labelGap),
        )
        DateBox(
            text = GastonomoTheme.formats.dates.long(value),
            look = look,
            // Sin actividad no hay donde ensenar el dialogo. No deberia pasar dentro de la
            // aplicacion; si pasara, el campo no hace nada en vez de cerrarla.
            onClick = {
                activity?.let { openPicker(it, DateRequest(value, minDate, maxDate), onValueChange) }
            },
        )
    }
}

@Composable
private fun DateBox(text: String, look: DateFieldLook, onClick: () -> Unit) {
    val shape = RoundedCornerShape(look.radius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(look.height)
            .background(GastonomoTheme.colors.surface, shape)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.inputBorder, shape)
            .pressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = look.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        GastonomoText(
            text = text,
            modifier = Modifier.weight(1f),
            style = look.valueStyle,
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
        GastonomoIcon(
            name = GastonomoIcons.DATE_RANGE,
            size = ICON_SIZE,
            tint = GastonomoTheme.colors.faint,
            contentDescription = stringResource(R.string.gastonomo_date_field_open),
        )
    }
}

/** Los tokens que cambian entre la variante de formulario y la del sheet de rango. */
private data class DateFieldLook(
    val labelStyle: TextStyle,
    val labelColor: Color,
    val labelGap: Dp,
    val height: Dp,
    val radius: Dp,
    val padding: Dp,
    val valueStyle: TextStyle,
)

@Composable
private fun lookOf(compact: Boolean): DateFieldLook = if (compact) {
    DateFieldLook(
        labelStyle = GastonomoTheme.typography.secondarySmall,
        labelColor = GastonomoTheme.colors.muted,
        labelGap = COMPACT_LABEL_GAP,
        height = COMPACT_HEIGHT,
        radius = GastonomoTheme.radii.monthPicker,
        padding = COMPACT_PADDING,
        valueStyle = GastonomoTheme.typography.bodySmall,
    )
} else {
    DateFieldLook(
        labelStyle = GastonomoTheme.typography.fieldLabel,
        labelColor = GastonomoTheme.colors.ink,
        labelGap = DATE_LABEL_GAP,
        height = GastonomoTheme.dimens.textField,
        radius = GastonomoTheme.radii.control,
        padding = DATE_FIELD_PADDING,
        valueStyle = GastonomoTheme.typography.body,
    )
}

/** La fecha actual del campo y sus limites, para no pasarle cinco parametros al selector. */
private data class DateRequest(
    val value: LocalDate,
    val minimum: LocalDate?,
    val maximum: LocalDate?,
)

private fun openPicker(activity: Activity, request: DateRequest, onPick: (LocalDate) -> Unit) {
    val dialog = DatePickerDialog(
        activity,
        { _, year, monthIndex, dayOfMonth -> onPick(LocalDate.of(year, monthIndex + 1, dayOfMonth)) },
        request.value.year,
        request.value.monthValue - 1,
        request.value.dayOfMonth,
    )
    request.minimum?.let { dialog.datePicker.minDate = it.toEpochMillis() }
    request.maximum?.let { dialog.datePicker.maxDate = it.toEpochMillis() }
    dialog.show()
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private val DATE_LABEL_GAP = 8.dp
private val COMPACT_LABEL_GAP = 6.dp
private val COMPACT_HEIGHT = 50.dp
private val DATE_FIELD_PADDING = 16.dp
private val COMPACT_PADDING = 12.dp
private val GAP = 8.dp
private val ICON_SIZE = 20.dp
