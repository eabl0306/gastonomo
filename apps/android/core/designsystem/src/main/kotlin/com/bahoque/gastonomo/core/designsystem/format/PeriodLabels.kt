package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.period.Period

/**
 * El label del selector de periodo, que Inicio y Movimientos ensenan igual: "Septiembre
 * 2026" en modo mes y "22 ago – 22 sept 2026" con un rango activo.
 */
fun GastonomoFormats.periodLabel(period: Period): String = when (period) {
    is Period.Month -> dates.monthAndYear(period.month)
    is Period.Range -> dates.range(period.from, period.to)
}
