package com.bahoque.gastonomo.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Umbrales de calidad propios del modulo. El nucleo hexagonal sube la cobertura a 80; el
 * resto hereda el minimo global verificado en el proyecto raiz.
 */
abstract class QualityExtension {

    abstract val lineCoverageMinimum: Property<Int>

    /**
     * Clases que no se pueden ejercitar sin un dispositivo real y que por eso no cuentan
     * para la cobertura. **No es una puerta trasera para bajar el umbral**: cada entrada
     * tiene que llevar en el build script el motivo por el que no se puede probar en la
     * JVM, y si algun dia se puede, se quita.
     */
    abstract val untestableOnJvm: ListProperty<String>
}
