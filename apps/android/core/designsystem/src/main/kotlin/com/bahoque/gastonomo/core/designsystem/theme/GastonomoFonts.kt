package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.bahoque.gastonomo.core.designsystem.R

/**
 * Plus Jakarta Sans en su fichero variable, con el eje wght. Una sola fuente de 172 KB
 * cubre los cinco pesos del diseno (400, 500, 600, 700 y 800); cinco estaticas pesarian
 * cerca de cinco veces mas.
 */
internal val PlusJakartaSans: FontFamily = FontFamily(
    weightedFont(FontWeight.Normal),
    weightedFont(FontWeight.Medium),
    weightedFont(FontWeight.SemiBold),
    weightedFont(FontWeight.Bold),
    weightedFont(FontWeight.ExtraBold),
)

/**
 * Material Symbols Rounded con FILL 0, recortada a los 61 iconos que usa el diseno.
 * El nombre del icono se escribe como texto y la ligadura de la fuente lo convierte en
 * glifo, igual que hace el prototipo.
 */
internal val MaterialSymbolsRounded: FontFamily =
    FontFamily(Font(R.font.material_symbols_rounded))

/** La misma fuente con FILL 1. El diseno la usa en el icono del tab activo. */
internal val MaterialSymbolsRoundedFilled: FontFamily =
    FontFamily(Font(R.font.material_symbols_rounded_filled))

// El constructor de Font con ejes variables sigue marcado como experimental en Compose.
// Es la unica forma de usar un solo ttf para los cinco pesos del diseno.
@OptIn(ExperimentalTextApi::class)
private fun weightedFont(weight: FontWeight): Font = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)
