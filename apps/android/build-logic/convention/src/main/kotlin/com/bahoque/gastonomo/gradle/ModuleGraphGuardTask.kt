package com.bahoque.gastonomo.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Direccion de dependencias entre modulos, leida del grafo de Gradle. Konsist mira los
 * imports, asi que una dependencia declarada y todavia sin usar se le escapa. Esta tarea
 * la ve, porque mira lo que declara el build script.
 */
abstract class ModuleGraphGuardTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencyPaths: SetProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val from = modulePath.get()
        val violations = dependencyPaths.get().sorted().mapNotNull { to ->
            forbiddenReason(from, to)?.let { "$from must not depend on $to: $it" }
        }
        writeReport(violations)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module graph guard found ${violations.size} violation(s):\n" + violations.joinToString("\n"),
            )
        }
    }

    private fun writeReport(violations: List<String>) {
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        val body = if (violations.isEmpty()) "OK: no module graph violations\n" else violations.joinToString("\n")
        report.writeText(body)
    }

    private companion object {
        const val FEATURE_PREFIX = ":feature:"
        const val DATA_PREFIX = ":data:"

        /** Capas de dentro afuera. Nadie puede depender de una capa mas externa que la suya. */
        val LAYER_RANK = linkedMapOf(
            ":core:common" to 0,
            ":core:domain" to 1,
            ":core:application" to 2,
            DATA_PREFIX to 3,
            FEATURE_PREFIX to 3,
            ":app" to 4,
        )

        fun forbiddenReason(from: String, to: String): String? {
            siblingAdapterReason(from, to)?.let { return it }
            val fromRank = rankOf(from) ?: return null
            val toRank = rankOf(to) ?: return null
            return if (toRank > fromRank) "an inner layer must not depend on an outer one" else null
        }

        fun siblingAdapterReason(from: String, to: String): String? = when {
            from.startsWith(FEATURE_PREFIX) && to.startsWith(DATA_PREFIX) ->
                "a driving adapter must not know a driven adapter"
            from.startsWith(DATA_PREFIX) && to.startsWith(FEATURE_PREFIX) ->
                "a driven adapter must not know a driving adapter"
            else -> null
        }

        fun rankOf(path: String): Int? =
            LAYER_RANK.entries.firstOrNull { path == it.key || path.startsWith(it.key) }?.value
    }
}
