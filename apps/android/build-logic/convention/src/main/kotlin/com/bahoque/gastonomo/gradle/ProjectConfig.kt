package com.bahoque.gastonomo.gradle

import org.gradle.api.JavaVersion

/**
 * Valores unicos de configuracion del proyecto Android. Los convention plugins leen de aqui
 * para que ningun modulo repita SDKs, versiones ni umbrales de calidad.
 */
internal object ProjectConfig {
    const val APPLICATION_ID = "com.bahoque.gastonomo"
    const val COMPILE_SDK = 37
    const val MIN_SDK = 29
    const val TARGET_SDK = 37
    const val VERSION_CODE = 1
    const val VERSION_NAME = "0.1.0"
    const val TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    const val JVM_TARGET = "17"

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

    /** Limite duro de lineas por fichero de codigo de produccion. */
    const val MAX_MAIN_FILE_LINES = 300

    /** Limite duro de lineas por fichero de test. */
    const val MAX_TEST_FILE_LINES = 400

    /** Cobertura minima agregada de todo el proyecto. */
    const val AGGREGATE_LINE_COVERAGE = 60

    /** Cobertura minima del nucleo hexagonal (dominio y casos de uso). */
    const val CORE_LINE_COVERAGE = 80

    /** URL base de la API para el emulador de Android. */
    const val DEBUG_API_BASE_URL = "http://10.0.2.2:8080"

    /** Placeholder de release: se sustituye por el host real cuando exista despliegue. */
    const val RELEASE_API_BASE_URL = "https://api.gastonomo.invalid"
}
