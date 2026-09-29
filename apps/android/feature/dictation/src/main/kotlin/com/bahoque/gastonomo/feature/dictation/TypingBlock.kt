package com.bahoque.gastonomo.feature.dictation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * El modo escrito: por que no se pudo escuchar, el campo de texto y los dos botones.
 *
 * Es la salida de todos los fallos del microfono, no una alternativa de segunda: quien no tiene
 * microfono, no dio permiso o esta sin conexion registra igual.
 */
@Composable
internal fun TypingBlock(state: DictateUiState, actions: DictateActions) {
    Column(modifier = Modifier.fillMaxWidth()) {
        state.micFailure?.let { failure ->
            NoticeLine(
                icon = GastonomoIcons.MIC_OFF,
                text = messageOf(failure),
                tint = GastonomoTheme.colors.amber,
            )
        }
        state.problem?.let { problem ->
            NoticeLine(
                icon = GastonomoIcons.INFO,
                text = messageOf(problem),
                tint = GastonomoTheme.colors.red,
            )
        }
        Spacer(Modifier.height(FIELD_GAP))
        GastonomoTextField(
            value = state.text,
            onValueChange = actions.onText,
            placeholder = stringResource(R.string.dictation_text_hint),
            imeAction = ImeAction.Done,
            lines = TEXT_LINES,
        )
        Spacer(Modifier.height(BUTTONS_GAP))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGapTight),
        ) {
            MicIconButton(onClick = actions.onListen)
            PrimaryButton(
                text = stringResource(R.string.dictation_interpret),
                onClick = { actions.onInterpret(state.text) },
                modifier = Modifier.weight(1f),
                enabled = state.text.isNotBlank(),
            )
        }
    }
}

/** La fase de interpretar: solo el aviso y lo que se dijo, entre comillas. */
@Composable
internal fun ProcessingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = PROCESSING_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GastonomoText(
            text = stringResource(R.string.dictation_processing),
            style = GastonomoTheme.typography.body,
            color = GastonomoTheme.colors.ink,
        )
        Spacer(Modifier.height(QUOTE_GAP))
        Quote(text)
    }
}

/** Lo que el usuario dijo, en gris y entre comillas. Sale al interpretar y al revisar. */
@Composable
internal fun Quote(text: String, modifier: Modifier = Modifier) {
    GastonomoText(
        text = "“$text”",
        modifier = modifier,
        style = GastonomoTheme.typography.bodySmall,
        color = GastonomoTheme.colors.muted,
    )
}

/** Una linea de aviso con su icono: el fallo del microfono o el de la interpretacion. */
@Composable
internal fun NoticeLine(
    icon: String,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = NOTICE_GAP),
        horizontalArrangement = Arrangement.spacedBy(NOTICE_ICON_GAP),
    ) {
        GastonomoIcon(name = icon, size = NOTICE_ICON, tint = tint)
        GastonomoText(
            text = text,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.noteText,
        )
    }
}

/**
 * El boton cuadrado de microfono que vuelve a escuchar desde el modo escrito.
 *
 * No es un [com.bahoque.gastonomo.core.designsystem.component.SecondaryButton] porque ese ocupa
 * todo el ancho por contrato, y aqui va al lado de Interpretar.
 */
@Composable
private fun MicIconButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.control)
    Box(
        modifier = Modifier
            .size(MIC_BUTTON)
            .background(GastonomoTheme.colors.surface, shape)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.inputBorder, shape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = GastonomoIcons.MIC,
            size = MIC_ICON,
            tint = GastonomoTheme.colors.ink,
            contentDescription = stringResource(R.string.dictation_start),
        )
    }
}

/** Los cuatro ejemplos que se pueden tocar para interpretar sin hablar ni escribir. */
@Composable
internal fun ExamplesBlock(onPick: (String) -> Unit) {
    val examples = listOf(
        stringResource(R.string.dictation_example_lunch),
        stringResource(R.string.dictation_example_taxi),
        stringResource(R.string.dictation_example_salary),
        stringResource(R.string.dictation_example_loan),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(top = EXAMPLES_GAP)) {
        GastonomoText(
            text = stringResource(R.string.dictation_examples),
            style = GastonomoTheme.typography.labelStrong,
            color = GastonomoTheme.colors.muted,
        )
        Spacer(Modifier.height(CHIPS_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(CHIPS_GAP)) {
            examples.forEach { example ->
                ExampleChip(text = example, onClick = { onPick(example) })
            }
        }
    }
}

/**
 * Un ejemplo, en una fila propia.
 *
 * El prototipo los reparte en varias columnas; aqui va uno por linea porque son frases enteras y
 * en un movil estrecho un flow las parte por la mitad.
 */
@Composable
private fun ExampleChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.pill)
    Box(
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .background(GastonomoTheme.colors.surface, shape)
            .border(GastonomoTheme.dimens.divider, GastonomoTheme.colors.inputBorder, shape)
            .pressable(onClick = onClick)
            .padding(horizontal = CHIP_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoText(
            text = text,
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.ink,
            maxLines = 1,
        )
    }
}

/** El campo del modo escrito admite tres lineas, que es lo que ocupa una frase dictada. */
private const val TEXT_LINES = 3

private val FIELD_GAP = 12.dp
private val BUTTONS_GAP = 14.dp
private val MIC_BUTTON = 56.dp
private val MIC_ICON = 24.dp
private val NOTICE_GAP = 8.dp
private val NOTICE_ICON_GAP = 8.dp
private val NOTICE_ICON = 18.dp
private val PROCESSING_PADDING = 28.dp
private val QUOTE_GAP = 10.dp
private val EXAMPLES_GAP = 22.dp
private val CHIPS_GAP = 8.dp
private val CHIP_HEIGHT = 40.dp
private val CHIP_PADDING = 14.dp
