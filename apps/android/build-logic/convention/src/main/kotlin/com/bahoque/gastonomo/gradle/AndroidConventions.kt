package com.bahoque.gastonomo.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Project

/**
 * Configuracion comun a todo modulo Android: SDKs, nivel de Java, BuildConfig y Lint estricto.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    with(extension) {
        compileSdk = ProjectConfig.COMPILE_SDK
        defaultConfig.minSdk = ProjectConfig.MIN_SDK
        compileOptions.sourceCompatibility = ProjectConfig.JAVA_VERSION
        compileOptions.targetCompatibility = ProjectConfig.JAVA_VERSION
        buildFeatures.buildConfig = true
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.isReturnDefaultValues = true
        configureLint(extension)
    }
}

/** AGP 9 genera siempre los informes de lint (html, xml y sarif), no hace falta activarlos. */
private fun configureLint(extension: CommonExtension) = extension.lint.configureLintRules()

/** Mismas reglas de lint para los modulos Android y para los modulos JVM del nucleo. */
internal fun Lint.configureLintRules() {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = true
    checkReleaseBuilds = true
    // Estos avisos dependen de lo que se haya publicado hoy en los repositorios:
    // no pueden romper una CI reproducible. Se ven, pero no fallan.
    informational += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
}
