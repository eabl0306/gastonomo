package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.settings.AppLanguage

/**
 * El sheet de idioma, compartido igual que [CurrencySheet]. Cambiar el idioma cambia los
 * textos, el formato de numeros, los nombres de meses y dias y los nombres de monedas.
 *
 * Los nombres de los dos idiomas van en su propia lengua y no se traducen: quien busca
 * "English" no sabe leer "Ingles".
 *
 * ```
 * if (state.sheet == Sheet.Language) {
 *     LanguageSheet(
 *         selected = state.language,
 *         onSelect = { viewModel.onSettingChanged(language = it) },
 *         onDismiss = viewModel::closeSheet,
 *     )
 * }
 * ```
 */
@Composable
fun LanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GastonomoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.gastonomo_language_title),
            subtitle = stringResource(R.string.gastonomo_language_subtitle),
        )
        Column(verticalArrangement = Arrangement.spacedBy(CARD_GAP)) {
            AppLanguage.entries.forEach { language ->
                RadioCard(
                    title = stringResource(GastonomoStrings.nameOf(language)),
                    subtitle = stringResource(language.regionRes()),
                    selected = language == selected,
                    onClick = { onSelect(language) },
                    radius = GastonomoTheme.radii.control,
                    modifier = Modifier.height(CARD_HEIGHT),
                )
            }
        }
    }
}

private fun AppLanguage.regionRes(): Int = when (this) {
    AppLanguage.SPANISH -> R.string.gastonomo_language_spanish_region
    AppLanguage.ENGLISH -> R.string.gastonomo_language_english_region
}

private val CARD_HEIGHT = 64.dp
private val CARD_GAP = 8.dp
