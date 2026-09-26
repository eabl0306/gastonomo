package com.bahoque.gastonomo.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoBottomSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoTextField
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.Segment
import com.bahoque.gastonomo.core.designsystem.component.SegmentedControl
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.component.tintOfCategoryColor
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * El sheet de crear o editar una categoria: vista previa, nombre, tipo, los 16 iconos y los
 * 8 colores del diseno.
 *
 * El error de nombre repetido se pinta en el subtitulo de la vista previa, que es donde lo
 * pone el prototipo, y no debajo del campo.
 */
@Composable
internal fun CategorySheet(
    form: CategoryForm,
    onFormChanged: (CategoryForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GastonomoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Preview(form)
        GastonomoTextField(
            value = form.name,
            onValueChange = { onFormChanged(form.copy(name = it)) },
            placeholder = stringResource(R.string.category_sheet_name),
            modifier = Modifier.padding(top = FIELD_GAP),
        )
        KindSegments(form, onFormChanged)
        SectionLabel(stringResource(R.string.category_sheet_icon))
        IconGrid(form, onFormChanged)
        SectionLabel(stringResource(R.string.category_sheet_color))
        ColorRow(form, onFormChanged)
        PrimaryButton(
            text = stringResource(
                if (form.isEditing) R.string.category_sheet_save else R.string.category_sheet_create,
            ),
            onClick = onSave,
            enabled = form.canSave,
            modifier = Modifier.padding(top = BUTTON_GAP),
        )
    }
}

@Composable
private fun Preview(form: CategoryForm) {
    val color = categoryColorOf(form.color)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PREVIEW_GAP),
    ) {
        IconTile(
            background = tintOfCategoryColor(color),
            size = GastonomoTheme.dimens.iconTileLarge,
            radius = GastonomoTheme.radii.previewTile,
        ) {
            GastonomoIcon(name = form.icon, size = PREVIEW_ICON, tint = color)
        }
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = stringResource(
                    if (form.isEditing) R.string.category_sheet_edit else R.string.category_sheet_new,
                ),
                style = GastonomoTheme.typography.sheetTitle,
                color = GastonomoTheme.colors.ink,
            )
            GastonomoText(
                text = previewSubtitle(form),
                style = GastonomoTheme.typography.secondary,
                color = if (form.hasNameProblem) {
                    GastonomoTheme.colors.red
                } else {
                    GastonomoTheme.colors.muted
                },
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun previewSubtitle(form: CategoryForm): String {
    val name = form.trimmedName.ifEmpty { stringResource(R.string.category_sheet_unnamed) }
    return when {
        form.fieldError != null -> form.fieldError
        form.duplicate -> stringResource(R.string.category_error_duplicate)
        else -> stringResource(
            R.string.category_sheet_preview,
            name,
            stringResource(form.kind.labelRes()),
        )
    }
}

@Composable
private fun KindSegments(form: CategoryForm, onFormChanged: (CategoryForm) -> Unit) {
    val kinds = listOf(MovementKind.OUT, MovementKind.IN)
    SegmentedControl(
        segments = kinds.map { Segment(label = stringResource(it.labelRes())) },
        selectedIndex = kinds.indexOf(form.kind),
        onSelect = { onFormChanged(form.copy(kind = kinds[it])) },
        modifier = Modifier.padding(top = FIELD_GAP),
        // Dentro del sheet el diseno usa la variante baja, no la de los formularios.
        compact = true,
    )
}

/** Los 16 iconos del diseno, en un grid de 8 por 2. */
@Composable
private fun IconGrid(form: CategoryForm, onFormChanged: (CategoryForm) -> Unit) {
    val selectedColor = categoryColorOf(form.color)
    Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        Category.ICON_CHOICES.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                row.forEach { icon ->
                    val selected = icon == form.icon
                    IconCell(
                        icon = icon,
                        selected = selected,
                        color = selectedColor,
                        onClick = { onFormChanged(form.copy(icon = icon)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCell(
    icon: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GastonomoTheme.radii.iconChoice)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (selected) color else GastonomoTheme.colors.surface)
            .border(
                width = GastonomoTheme.dimens.selectedBorder,
                color = if (selected) color else GastonomoTheme.colors.line,
                shape = shape,
            )
            .pressable(onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        GastonomoIcon(
            name = icon,
            size = CELL_ICON,
            tint = if (selected) GastonomoTheme.colors.surface else GastonomoTheme.colors.ink,
        )
    }
}

/** Los 8 colores del diseno. El elegido lleva doble anillo y un check blanco. */
@Composable
private fun ColorRow(form: CategoryForm, onFormChanged: (CategoryForm) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Category.COLOR_CHOICES.forEach { hex ->
            ColorSwatch(
                color = categoryColorOf(hex),
                selected = hex == form.color,
                onClick = { onFormChanged(form.copy(color = hex)) },
            )
        }
    }
}

/**
 * Un circulo de color. El anillo del elegido se dibuja **fuera** del circulo, como el
 * `box-shadow` del diseno, para que el seleccionado no salga mas pequeno que los demas.
 */
@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ringColor = GastonomoTheme.colors.bgNav
    val size = GastonomoTheme.dimens.colorSwatch
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                if (!selected) return@drawBehind
                val radius = size.toPx() / 2
                drawCircle(ringColor, radius + RING_WIDTH.toPx() / 2, style = Stroke(RING_WIDTH.toPx()))
                drawCircle(
                    color = color,
                    radius = radius + RING_WIDTH.toPx() * 1.5f,
                    style = Stroke(RING_WIDTH.toPx()),
                )
            }
            .background(color, CircleShape)
            .pressable(onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            GastonomoIcon(
                name = GastonomoIcons.CHECK,
                size = CHECK_SIZE,
                tint = GastonomoTheme.colors.surface,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    GastonomoText(
        text = text,
        style = GastonomoTheme.typography.secondarySmall,
        color = GastonomoTheme.colors.muted,
        modifier = Modifier.padding(top = LABEL_TOP, bottom = LABEL_BOTTOM),
    )
}

/** "Gasto" o "Ingreso", que son textos comunes y no de esta pantalla. */
internal fun MovementKind.labelRes(): Int = when (this) {
    MovementKind.OUT -> GastonomoStrings.kindExpense
    MovementKind.IN -> GastonomoStrings.kindIncome
}

private const val COLUMNS = 8
private val PREVIEW_GAP = 12.dp
private val PREVIEW_ICON = 26.dp
private val FIELD_GAP = 12.dp
private val CELL_GAP = 6.dp
private val CELL_ICON = 19.dp
private val CHECK_SIZE = 18.dp
private val RING_WIDTH = 2.dp
private val LABEL_TOP = 16.dp
private val LABEL_BOTTOM = 8.dp
private val BUTTON_GAP = 20.dp
