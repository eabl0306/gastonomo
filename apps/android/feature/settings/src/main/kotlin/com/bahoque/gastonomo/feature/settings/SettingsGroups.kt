package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.Overline
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** El grupo Cuenta: sincronizacion, cerrojo local y cambio de contrasena. */
@Composable
internal fun AccountGroup(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(stringResource(R.string.settings_group_account), modifier) {
        val synced = state.cloudSync == CloudSyncState.SYNCED
        SettingsValueRow(
            icon = GastonomoIcons.CLOUD_DONE,
            label = stringResource(R.string.settings_cloud_sync),
            value = stringResource(
                if (synced) R.string.settings_cloud_sync_ok else R.string.settings_cloud_sync_paused,
            ),
            valueColor = if (synced) GastonomoTheme.colors.green else GastonomoTheme.colors.muted,
        )
        SettingsToggleRow(
            icon = GastonomoIcons.FINGERPRINT,
            label = stringResource(R.string.settings_lock),
            checked = state.device.lockEnabled,
            onCheckedChange = actions.onLockToggled,
        )
        SettingsValueRow(
            icon = GastonomoIcons.LOCK,
            label = stringResource(R.string.settings_change_password),
            showDivider = false,
        )
    }
}

/**
 * El grupo General: moneda, idioma y tasas. La nota de debajo explica que cambiar la moneda
 * no toca ningun registro guardado.
 *
 * "Inicio del mes" se quito del diseno a proposito y no vuelve.
 */
@Composable
internal fun GeneralGroup(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsGroup(stringResource(R.string.settings_group_general)) {
            val currency = state.settings.displayCurrency
            SettingsValueRow(
                icon = GastonomoIcons.PAYMENTS,
                label = stringResource(R.string.settings_currency),
                value = stringResource(
                    R.string.settings_currency_value,
                    currency.value,
                    GastonomoTheme.formats.currencyNames.of(currency),
                ),
                showChevron = true,
                onClick = actions.onCurrency,
            )
            SettingsValueRow(
                icon = GastonomoIcons.LANGUAGE,
                label = stringResource(R.string.settings_language),
                value = stringResource(GastonomoStrings.nameOf(state.settings.language)),
                showChevron = true,
                onClick = actions.onLanguage,
            )
            RatesRow(state.rates)
        }
        GastonomoText(
            text = stringResource(R.string.settings_general_note),
            style = GastonomoTheme.typography.secondarySmall,
            color = GastonomoTheme.colors.muted,
            modifier = Modifier.padding(top = NOTE_GAP, start = NOTE_SIDE, end = NOTE_SIDE),
        )
    }
}

/** El grupo Datos: categorias y exportacion. */
@Composable
internal fun DataGroup(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(stringResource(R.string.settings_group_data), modifier) {
        SettingsValueRow(
            icon = GastonomoIcons.SELL,
            label = stringResource(R.string.settings_categories),
            value = state.categoryCount.toString(),
            showChevron = true,
            onClick = actions.onCategories,
        )
        SettingsValueRow(
            icon = GastonomoIcons.DOWNLOAD,
            label = stringResource(R.string.settings_export),
            value = stringResource(R.string.settings_export_value),
            showChevron = true,
            showDivider = false,
            onClick = actions.onExport,
        )
    }
}

/** La fila de tasas es de solo lectura: la fecha en verde, o el estado en muted. */
@Composable
private fun RatesRow(rates: RatesIndicator) {
    val ready = rates as? RatesIndicator.Ready
    val date = ready?.date
    val value = when {
        date != null -> stringResource(
            R.string.settings_rates_ready,
            GastonomoTheme.formats.dates.short(date),
        )
        rates is RatesIndicator.Loading -> stringResource(GastonomoStrings.loading)
        else -> stringResource(R.string.settings_rates_offline)
    }
    SettingsValueRow(
        icon = GastonomoIcons.CURRENCY_EXCHANGE,
        label = stringResource(R.string.settings_rates),
        value = value,
        valueColor = if (date != null) GastonomoTheme.colors.green else GastonomoTheme.colors.muted,
        showDivider = false,
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Overline(title, modifier = Modifier.padding(bottom = OVERLINE_GAP))
        GastonomoCard { content() }
    }
}

private val OVERLINE_GAP = 8.dp
private val NOTE_GAP = 8.dp
private val NOTE_SIDE = 4.dp
