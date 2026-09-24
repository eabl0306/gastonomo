package com.bahoque.gastonomo.gradle

import org.gradle.api.provider.Property

/**
 * Umbral de cobertura propio del modulo. El nucleo hexagonal lo sube a 80; el resto
 * hereda el minimo global verificado en el proyecto raiz.
 */
abstract class QualityExtension {
    abstract val lineCoverageMinimum: Property<Int>
}
