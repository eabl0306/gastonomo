package com.bahoque.gastonomo.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.designsystem.R

/**
 * "Nada pendiente", "1 prestamo" o "N prestamos".
 *
 * Es la misma etiqueta en las tarjetas de Inicio y en las de Prestamos, asi que vive aqui y no
 * en una de las dos features: estaba copiada con el mismo texto en dos `strings.xml`.
 */
@Composable
fun loanCountLabel(count: Int): String = if (count == 0) {
    stringResource(R.string.gastonomo_loan_count_none)
} else {
    pluralStringResource(R.plurals.gastonomo_loan_count, count, count)
}
