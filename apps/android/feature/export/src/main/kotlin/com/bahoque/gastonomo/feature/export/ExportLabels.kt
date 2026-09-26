package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanStatus
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/** El nombre de una hoja y sus encabezados, ya traducidos. */
data class ExportTableLabels(val name: String, val headers: List<String>)

/**
 * Todos los textos que acaban dentro del archivo exportado.
 *
 * Se resuelven en la pantalla, que es quien tiene los recursos, y se le pasan al ViewModel
 * al pulsar Exportar: asi el archivo sale en el idioma de la aplicacion sin que el nucleo
 * ni el ViewModel conozcan `R.string`.
 */
data class ExportLabels(
    val movements: ExportTableLabels,
    val loans: ExportTableLabels,
    val payments: ExportTableLabels,
    val income: String,
    val expense: String,
    val owedToMe: String,
    val iOwe: String,
    val paid: String,
    val partial: String,
    val pending: String,
    val payment: String,
) {
    fun kindOf(kind: MovementKind): String = if (kind.isIncome) income else expense

    fun directionOf(direction: LoanDirection): String =
        if (direction == LoanDirection.LENT) owedToMe else iOwe

    fun statusOf(status: LoanStatus): String = when (status) {
        LoanStatus.PAID -> paid
        LoanStatus.PARTIAL -> partial
        LoanStatus.PENDING -> pending
    }
}
