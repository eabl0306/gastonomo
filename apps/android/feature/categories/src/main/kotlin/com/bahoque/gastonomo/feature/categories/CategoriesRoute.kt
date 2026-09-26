package com.bahoque.gastonomo.feature.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** Categorias con su ViewModel. Solo esto conoce la navegacion. */
@Composable
fun CategoriesRoute(
    dependencies: CategoriesDependencies,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel { CategoriesViewModel(dependencies) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack) {
        CategoriesActions(
            onBack = onBack,
            onTab = viewModel::onTabSelected,
            onOpenSheet = viewModel::onOpenSheet,
            onDelete = viewModel::onDelete,
            onRetry = viewModel::load,
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        CategoriesScreen(state = state, actions = actions)
        state.form?.let { form ->
            CategorySheet(
                form = form,
                onFormChanged = viewModel::onFormChanged,
                onSave = viewModel::onSave,
                onDismiss = viewModel::onCloseSheet,
            )
        }
        state.message?.let { message ->
            GastonomoToast(
                message = message.text(),
                onHide = viewModel::onMessageShown,
                icon = if (message.isError) GastonomoIcons.INFO else GastonomoIcons.CHECK_CIRCLE,
                iconTint = if (message.isError) {
                    GastonomoTheme.colors.amber
                } else {
                    GastonomoTheme.colors.lime
                },
            )
        }
    }
}

/** El texto de cada aviso. Se resuelve aqui: el ViewModel no conoce los recursos. */
@Composable
private fun CategoriesMessage.text(): String = when (this) {
    is CategoriesMessage.Created -> stringResource(R.string.categories_toast_created, name)
    CategoriesMessage.Updated -> stringResource(R.string.categories_toast_updated)
    is CategoriesMessage.Deleted -> stringResource(R.string.categories_toast_deleted, name)
    CategoriesMessage.IsDefault -> stringResource(R.string.categories_error_default)
    CategoriesMessage.SaveFailed -> stringResource(R.string.categories_error_save)
}
