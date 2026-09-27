package com.bahoque.gastonomo.feature.settings

import androidx.compose.runtime.Immutable

/**
 * Lo que se puede tocar en Ajustes. Se agrupa en un valor para que la pantalla no reciba
 * seis lambdas sueltas y para que un test pueda pasar dobles vacios.
 */
@Immutable
data class SettingsActions(
    val onCurrency: () -> Unit,
    val onLanguage: () -> Unit,
    val onLockToggled: (Boolean) -> Unit,
    val onCategories: () -> Unit,
    val onExport: () -> Unit,

    /** Abre la lista de registros que todavia no han salido del telefono. */
    val onPending: () -> Unit,
    val onSignOut: () -> Unit,
) {
    companion object {
        /** Acciones que no hacen nada, para las capturas y los tests de pintado. */
        val NONE = SettingsActions({}, {}, {}, {}, {}, {}, {})
    }
}
