package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS

/**
 * El telefono del prototipo, y ademas en espanol.
 *
 * Robolectric arranca en `en-US`, asi que sin el prefijo de idioma los tests leerian
 * `values-en` y las capturas saldrian en ingles. El diseno entregado esta en espanol.
 */
const val SPANISH_QUALIFIERS = "es-rCO-$GASTONOMO_QUALIFIERS"
