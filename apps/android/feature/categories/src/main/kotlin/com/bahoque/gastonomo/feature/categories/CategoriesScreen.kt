package com.bahoque.gastonomo.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoEmptyStateRetry
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.Overline
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.component.Segment
import com.bahoque.gastonomo.core.designsystem.component.SegmentedControl
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * La pantalla 13: el segmented de Gastos e Ingresos con su conteo, las categorias del
 * usuario, las 12 predeterminadas y el boton fijo de crear una.
 *
 * Una categoria de tipo `both` sale en los dos tabs, que es lo que hace el prototipo con
 * Prestamo, Abono prestamo y Otros.
 */
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    actions: CategoriesActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        GastonomoAppBar(
            title = stringResource(R.string.categories_title),
            onNavigate = actions.onBack,
        )
        GastonomoScreen(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
            ) {
                KindTabs(state, actions.onTab)
                if (state.loadFailed) {
                    GastonomoEmptyStateRetry(
                        icon = GastonomoIcons.SELL,
                        message = stringResource(R.string.categories_error_load),
                        onRetry = actions.onRetry,
                    )
                } else {
                    CategoryGroups(state, actions)
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.categories_new),
            onClick = { actions.onOpenSheet(null) },
            icon = GastonomoIcons.ADD,
            modifier = Modifier.padding(
                start = GastonomoTheme.dimens.screenPadding,
                end = GastonomoTheme.dimens.screenPadding,
                top = BUTTON_TOP,
                bottom = BUTTON_BOTTOM,
            ),
        )
    }
}

@Composable
private fun KindTabs(state: CategoriesUiState, onTab: (MovementKind) -> Unit) {
    val tabs = listOf(MovementKind.OUT, MovementKind.IN)
    SegmentedControl(
        segments = tabs.map { kind ->
            Segment(
                label = stringResource(kind.tabLabelRes()),
                caption = state.countOf(kind).toString(),
            )
        },
        selectedIndex = tabs.indexOf(state.tab),
        onSelect = { onTab(tabs[it]) },
        modifier = Modifier.padding(top = TABS_TOP),
    )
}

@Composable
private fun CategoryGroups(state: CategoriesUiState, actions: CategoriesActions) {
    if (state.mine.isNotEmpty()) {
        CategoryGroup(
            title = stringResource(R.string.categories_mine),
            categories = state.mine,
        ) { category, last ->
            MyCategoryRow(
                category = category,
                onEdit = { actions.onOpenSheet(category) },
                onArchive = { actions.onArchive(category) },
                showDivider = !last,
            )
        }
    }
    CategoryGroup(
        title = stringResource(R.string.categories_default),
        categories = state.defaults,
    ) { category, last ->
        DefaultCategoryRow(category = category, showDivider = !last)
    }
    if (state.archived.isNotEmpty()) {
        ArchivedGroup(state.archived, actions.onRestore)
    }
}

/**
 * Las archivadas, al final y con su explicacion.
 *
 * Va debajo de las predeterminadas porque es lo que menos se mira: solo se entra aqui a recuperar
 * algo. La nota de arriba es la que evita la pregunta de por que sigue en la lista una categoria
 * que se borro.
 */
@Composable
private fun ArchivedGroup(archived: List<Category>, onRestore: (Category) -> Unit) {
    Column {
        Overline(
            stringResource(R.string.categories_archived),
            modifier = Modifier.padding(bottom = OVERLINE_GAP),
        )
        GastonomoText(
            text = stringResource(R.string.categories_archived_note),
            style = GastonomoTheme.typography.secondarySmall,
            color = GastonomoTheme.colors.muted,
            modifier = Modifier.padding(start = NOTE_PADDING, end = NOTE_PADDING, bottom = OVERLINE_GAP),
        )
        GastonomoCard {
            archived.forEachIndexed { index, category ->
                ArchivedCategoryRow(
                    category = category,
                    onRestore = { onRestore(category) },
                    showDivider = index != archived.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun CategoryGroup(
    title: String,
    categories: List<Category>,
    row: @Composable (Category, Boolean) -> Unit,
) {
    Column {
        Overline(title, modifier = Modifier.padding(bottom = OVERLINE_GAP))
        GastonomoCard {
            categories.forEachIndexed { index, category ->
                row(category, index == categories.lastIndex)
            }
        }
    }
}

/** El label de cada tab va en plural: "Gastos" e "Ingresos". */
private fun MovementKind.tabLabelRes(): Int = when (this) {
    MovementKind.OUT -> R.string.categories_tab_expenses
    MovementKind.IN -> R.string.categories_tab_income
}

private val SECTION_GAP = 14.dp
private val TABS_TOP = 4.dp
private val OVERLINE_GAP = 8.dp
private val NOTE_PADDING = 4.dp
private val BUTTON_TOP = 12.dp
private val BUTTON_BOTTOM = 24.dp
