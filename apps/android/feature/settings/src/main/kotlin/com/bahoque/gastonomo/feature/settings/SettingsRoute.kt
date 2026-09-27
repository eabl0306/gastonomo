package com.bahoque.gastonomo.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheet
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheetOrigin
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.component.LanguageSheet
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Ajustes con su ViewModel. Es lo unico de este modulo que conoce el grafo de navegacion:
 * la pantalla en si no sabe a donde van sus filas.
 */
@Composable
fun SettingsRoute(
    dependencies: SettingsDependencies,
    onCategories: () -> Unit,
    onExport: () -> Unit,
    onPending: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel { SettingsViewModel(dependencies) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onCategories, onExport, onPending, onSignedOut) {
        SettingsActions(
            onCurrency = { viewModel.showSheet(SettingsSheet.CURRENCY) },
            onLanguage = { viewModel.showSheet(SettingsSheet.LANGUAGE) },
            onLockToggled = viewModel::onLockToggled,
            onCategories = onCategories,
            onExport = onExport,
            onPending = onPending,
            onSignOut = { viewModel.signOut(onSignedOut) },
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        SettingsScreen(state = state, actions = actions, version = appVersion())
        SettingsSheets(state = state, viewModel = viewModel)
        state.message?.let { SettingsToast(message = it, onHide = viewModel::onMessageShown) }
    }
}

@Composable
private fun SettingsSheets(state: SettingsUiState, viewModel: SettingsViewModel) {
    // El host retiene cual era la hoja mientras baja: sin eso, cerrarla dejaria de pintar su
    // contenido antes de que se vea salir.
    GastonomoSheetHost(state.sheet) { sheet ->
        SettingsSheetContent(sheet = sheet, state = state, viewModel = viewModel)
    }
}

@Composable
private fun SettingsSheetContent(
    sheet: SettingsSheet,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    when (sheet) {
        SettingsSheet.CURRENCY -> CurrencySheet(
            origin = CurrencySheetOrigin.Settings,
            currencies = state.currencies,
            selected = state.settings.displayCurrency,
            query = state.currencyQuery,
            onQueryChange = viewModel::onCurrencyQuery,
            onSelect = viewModel::onCurrencySelected,
            onDismiss = { viewModel.showSheet(null) },
        )
        SettingsSheet.LANGUAGE -> LanguageSheet(
            selected = state.settings.language,
            onSelect = viewModel::onLanguageSelected,
            onDismiss = { viewModel.showSheet(null) },
        )
        SettingsSheet.PIN -> PinSheet(
            pin = state.pin,
            onPinChanged = viewModel::onPinChanged,
            onConfirm = viewModel::confirmPin,
            onDismiss = { viewModel.showSheet(null) },
        )
    }
}

@Composable
private fun SettingsToast(message: SettingsMessage, onHide: () -> Unit) {
    GastonomoToast(
        message = message.text(),
        onHide = onHide,
        icon = if (message.isError) GastonomoIcons.INFO else GastonomoIcons.CHECK_CIRCLE,
        iconTint = if (message.isError) GastonomoTheme.colors.amber else GastonomoTheme.colors.lime,
    )
}

/** El texto de cada aviso. Vive aqui y no en el ViewModel, que no conoce los recursos. */
@Composable
private fun SettingsMessage.text(): String = when (this) {
    is SettingsMessage.CurrencyChanged ->
        stringResource(R.string.settings_toast_currency, currency.value)
    SettingsMessage.LockEnabled -> stringResource(R.string.settings_toast_lock_on)
    SettingsMessage.LockDisabled -> stringResource(R.string.settings_toast_lock_off)
    SettingsMessage.CatalogNotReady -> stringResource(R.string.settings_error_catalog)
    SettingsMessage.SaveFailed -> stringResource(R.string.settings_error_save)
}

/**
 * La version que ensena el pie. Sale del paquete instalado, no de una constante, para que no
 * se quede vieja; en un test de libreria no hay version y entonces se usa la del proyecto.
 */
@Composable
private fun appVersion(): String {
    val context: Context = LocalContext.current
    return remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: FALLBACK_VERSION
    }
}

/** La version del proyecto, para cuando el paquete no la declara. */
private const val FALLBACK_VERSION = "0.1.0"
