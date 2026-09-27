package com.bahoque.gastonomo.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Arquitectura hexagonal verificada como test. Si alguien invierte una dependencia, esto
 * falla en CI igual que un test de negocio.
 *
 * Konsist mira los imports. Lo que no ve, una dependencia declarada y todavia sin usar, lo
 * ve la tarea `moduleGraphGuard`, que lee el grafo real de Gradle. Hacen falta las dos.
 */
class HexagonalArchitectureTest {

    @Test
    fun `the core does not know android`() {
        Konsist.scopeFromDirectories(CORE).files.assertFalse { it.importsAndroid() }
    }

    @Test
    fun `the core does not know the data adapters`() {
        Konsist.scopeFromDirectories(CORE).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `the core does not know the features`() {
        Konsist.scopeFromDirectories(CORE).files.assertFalse { it.importsFeatures() }
    }

    @Test
    fun `no feature knows the data adapters`() {
        Konsist.scopeFromDirectories(FEATURES).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `no feature knows another feature`() {
        Konsist.scopeFromDirectories(FEATURES).files.assertTrue { file ->
            file.foreignFeatureImports().isEmpty()
        }
    }

    @Test
    fun `no data adapter knows a feature or the design system`() {
        Konsist.scopeFromDirectories(DATA).files.assertFalse {
            it.importsFeatures() || it.importsDesignSystem()
        }
    }

    @Test
    fun `the design system does not know the data adapters`() {
        Konsist.scopeFromDirectory(DESIGN_SYSTEM).files.assertFalse { it.importsDataAdapters() }
    }

    @Test
    fun `no view model lives outside a feature module`() {
        Konsist.scopeFromDirectories(CORE + DATA + listOf(APP))
            .classes()
            .assertFalse { it.name.endsWith(VIEW_MODEL_SUFFIX) }
    }

    @Test
    fun `every view model of a feature is named after its role`() {
        Konsist.scopeFromDirectories(FEATURES)
            .classes()
            .filter { it.text.contains(VIEW_MODEL_SUPERTYPE) }
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
        val CORE = listOf(
            "core/common/src/main/kotlin",
            "core/domain/src/main/kotlin",
            "core/application/src/main/kotlin",
        )
        val DATA = listOf(
            "data/database/src/main/kotlin",
            "data/offline/src/main/kotlin",
            "data/network/src/main/kotlin",
            "data/session/src/main/kotlin",
            "data/auth/src/main/kotlin",
            "data/finance/src/main/kotlin",
            "data/catalog/src/main/kotlin",
        )
        val FEATURES = listOf(
            "feature/auth/src/main/kotlin",
            "feature/home/src/main/kotlin",
            "feature/movements/src/main/kotlin",
            "feature/loans/src/main/kotlin",
            "feature/settings/src/main/kotlin",
            "feature/categories/src/main/kotlin",
            "feature/export/src/main/kotlin",
        )
        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin"
        const val USE_CASES = "core/application/src/main/kotlin/com/bahoque/gastonomo/core/application/usecase"
        const val APP = "app/src/main/kotlin"
        const val VIEW_MODEL_SUFFIX = "ViewModel"
        const val VIEW_MODEL_SUPERTYPE = ": ViewModel("
        const val INVOKE = "invoke"
        val ANDROID_PACKAGES = listOf("android.", "androidx.")
        const val DATA_PACKAGE = "com.bahoque.gastonomo.data."
        const val FEATURE_PACKAGE = "com.bahoque.gastonomo.feature."
        const val DESIGN_SYSTEM_PACKAGE = "com.bahoque.gastonomo.core.designsystem."

        fun KoFileDeclaration.importsAndroid(): Boolean =
            imports.any { import -> ANDROID_PACKAGES.any { import.name.startsWith(it) } }

        fun KoFileDeclaration.importsDataAdapters(): Boolean =
            imports.any { it.name.startsWith(DATA_PACKAGE) }

        fun KoFileDeclaration.importsFeatures(): Boolean =
            imports.any { it.name.startsWith(FEATURE_PACKAGE) }

        fun KoFileDeclaration.importsDesignSystem(): Boolean =
            imports.any { it.name.startsWith(DESIGN_SYSTEM_PACKAGE) }

        /** Los imports de otra feature distinta de la del propio fichero. */
        fun KoFileDeclaration.foreignFeatureImports(): List<String> {
            val own = FEATURE_PACKAGE + path.substringAfter("feature/").substringBefore('/')
            return imports.map { it.name }
                .filter { it.startsWith(FEATURE_PACKAGE) && !it.startsWith(own) }
        }
    }
}
