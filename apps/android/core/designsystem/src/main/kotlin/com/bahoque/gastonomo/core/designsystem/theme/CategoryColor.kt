package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * El color de una categoria, que el dominio guarda como `#rrggbb` porque es lo que manda la
 * API, traducido al Color de Compose.
 *
 * Un valor que no se puede leer cae en el gris de "Otros" en vez de romper la pantalla: el
 * fondo de un tile no justifica tirar una excepcion en mitad de una lista.
 *
 * ```
 * val color = categoryColorOf(movement.category.color)
 * IconTile(background = tintOfCategoryColor(color)) { GastonomoIcon(icon, tint = color) }
 * ```
 */
fun categoryColorOf(hex: String): Color {
    val digits = hex.removePrefix(HASH)
    val value = digits.toLongOrNull(HEX_RADIX) ?: return FALLBACK_CATEGORY_COLOR
    return when (digits.length) {
        RGB_DIGITS -> Color(value or OPAQUE_ALPHA)
        ARGB_DIGITS -> Color(value)
        else -> FALLBACK_CATEGORY_COLOR
    }
}

/** El gris de la categoria "Otros". Es el color al que cae el prototipo cuando no reconoce una. */
val FALLBACK_CATEGORY_COLOR: Color = Color(0xFF7C7669)

private const val HASH = "#"
private const val HEX_RADIX = 16
private const val RGB_DIGITS = 6
private const val ARGB_DIGITS = 8
private const val OPAQUE_ALPHA = 0xFF000000L
