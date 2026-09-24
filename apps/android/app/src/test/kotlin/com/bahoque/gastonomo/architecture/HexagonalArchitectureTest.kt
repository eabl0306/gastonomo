package com.bahoque.gastonomo.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Arquitectura hexagonal verificada como test. Si alguien invierte una dependencia,
 * esto falla en CI igual que un test de negocio.
 */
class HexagonalArchitectureTest {

    @Test
    fun `the domain does not know android`() {
        Konsist.scopeFromDirectory(DOMAIN).files.assertFalse { it.importsAndroid() }
    }

    @Test
    fun `the use cases do not know android`() {
        Konsist.scopeFromDirectory(APPLICATION).files.assertFalse { it.importsAndroid() }
    }

    @Test
    fun `the use cases do not know the data adapters`() {
        Konsist.scopeFromDirectory(APPLICATION).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `the domain does not know the data adapters`() {
        Konsist.scopeFromDirectory(DOMAIN).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `the feature does not know the data adapters`() {
        Konsist.scopeFromDirectory(FEATURE).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `no view model lives outside a feature module`() {
        Konsist.scopeFromDirectories(listOf(DOMAIN, APPLICATION, DATA, APP))
            .classes()
            .assertFalse { it.name.endsWith(VIEW_MODEL_SUFFIX) }
    }

    @Test
    fun `every view model of the feature is named after its role`() {
        Konsist.scopeFromDirectory(FEATURE)
            .classes()
            .filter { it.text.contains(": ViewModel(") }
            .assertTrue { it.name.endsWith(VIEW_MODEL_SUFFIX) }
    }

    @Test
    fun `every use case exposes a single public function named invoke`() {
        Konsist.scopeFromDirectory(USE_CASES)
            .classes()
            .assertTrue { useCase ->
                val public = useCase.functions().filter { it.hasPublicOrDefaultModifier }
                public.size == 1 && public.single().name == INVOKE
            }
    }

    private companion object {
        const val DOMAIN = "core/domain/src/main/kotlin"
        const val APPLICATION = "core/application/src/main/kotlin"
        const val USE_CASES = "core/application/src/main/kotlin/com/bahoque/gastonomo/core/application/health/usecase"
        const val DATA = "data/health/src/main/kotlin"
        const val FEATURE = "feature/health/src/main/kotlin"
        const val APP = "app/src/main/kotlin"
        const val VIEW_MODEL_SUFFIX = "ViewModel"
        const val INVOKE = "invoke"
        val ANDROID_PACKAGES = listOf("android.", "androidx.")
        const val DATA_PACKAGE = "com.bahoque.gastonomo.data."

        fun KoFileDeclaration.importsAndroid(): Boolean =
            imports.any { import -> ANDROID_PACKAGES.any { import.name.startsWith(it) } }

        fun KoFileDeclaration.importsDataAdapters(): Boolean =
            imports.any { it.name.startsWith(DATA_PACKAGE) }
    }
}
