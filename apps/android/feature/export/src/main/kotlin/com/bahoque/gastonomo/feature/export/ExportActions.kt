package com.bahoque.gastonomo.feature.export

import androidx.compose.runtime.Immutable

/** Lo que se puede tocar en Exportar, agrupado para no pasar seis lambdas sueltas. */
@Immutable
data class ExportActions(
    val onBack: () -> Unit,
    val onFormat: (ExportFormat) -> Unit,
    val onToggleSet: (ExportSet) -> Unit,
    val onRange: (ExportRange) -> Unit,
    val onExport: () -> Unit,
    val onRetry: () -> Unit,
) {
    companion object {
        /** Acciones que no hacen nada, para las capturas y los tests de pintado. */
        val NONE = ExportActions({}, {}, {}, {}, {}, {})
    }
}
