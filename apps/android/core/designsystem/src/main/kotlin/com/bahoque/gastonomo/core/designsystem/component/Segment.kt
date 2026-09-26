package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.ui.graphics.Color

/**
 * Una opcion de un [SegmentedControl].
 *
 * @param activeColor el color del texto del segmento activo. El diseno lo cambia segun el
 *   tipo: verde en Ingreso, rojo en Gasto, ink en el resto.
 * @param caption el conteo que ensena el segmented control de Categorias.
 */
data class Segment(
    val label: String,
    val icon: String? = null,
    val activeColor: Color? = null,
    val caption: String? = null,
)
