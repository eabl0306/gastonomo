package com.bahoque.gastonomo.feature.auth

import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS

/**
 * El tamano del prototipo, 390x844 a 3x, mas el idioma por defecto de la aplicacion.
 *
 * Robolectric arranca en ingles: sin el `es-rCO` las pantallas saldrian con los textos de
 * `values-en` y las capturas no se podrian comparar con el diseno.
 */
internal const val SPANISH_QUALIFIERS = "es-rCO-" + GASTONOMO_QUALIFIERS
