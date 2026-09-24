package com.bahoque.gastonomo.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Reglas de calidad que detekt no cubre: tamano maximo de fichero y prohibicion de
 * tests saltados o enfocados sin justificacion. Falla el build ante cualquier violacion.
 */
@CacheableTask
abstract class SourceGuardTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val maxMainFileLines: Property<Int>

    @get:Input
    abstract val maxTestFileLines: Property<Int>

    @get:Input
    abstract val projectDirectory: Property<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val root = projectDirectory.get()
        val violations = sourceFiles.files
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .flatMap { inspect(it, root) }
        writeReport(violations)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Source guard found ${violations.size} violation(s):\n" + violations.joinToString("\n"),
            )
        }
    }

    private fun writeReport(violations: List<String>) {
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        val body = if (violations.isEmpty()) "OK: no source guard violations\n" else violations.joinToString("\n")
        report.writeText(body)
    }

    private fun inspect(file: File, root: String): List<String> {
        val lines = file.readLines()
        val relative = file.path.removePrefix(root).removePrefix("/")
        return lengthViolations(relative, lines.size, isTest(file)) +
            skippedTestViolations(relative, lines) +
            suppressViolations(relative, lines)
    }

    private fun isTest(file: File): Boolean =
        file.path.contains("/src/test/") || file.path.contains("/src/androidTest/")

    private fun lengthViolations(relative: String, size: Int, isTest: Boolean): List<String> {
        val limit = if (isTest) maxTestFileLines.get() else maxMainFileLines.get()
        if (size <= limit) return emptyList()
        return listOf("$relative: file has $size lines, limit is $limit")
    }

    private fun skippedTestViolations(relative: String, lines: List<String>): List<String> =
        lines.mapIndexedNotNull { index, line -> skipReason(line)?.let { "$relative:${index + 1}: $it" } }

    /** Silenciar una regla exige decir por que: comentario en la misma linea o justo encima. */
    private fun suppressViolations(relative: String, lines: List<String>): List<String> =
        lines.mapIndexedNotNull { index, line ->
            val match = SUPPRESS_ANNOTATION.find(line) ?: return@mapIndexedNotNull null
            val trailing = line.substring(match.range.last)
            val justified = JUSTIFICATION.containsMatchIn(trailing) ||
                JUSTIFICATION.containsMatchIn(lines.getOrElse(index - 1) { "" }.trim())
            if (justified) null else "$relative:${index + 1}: @Suppress without a written reason"
        }

    private fun skipReason(line: String): String? {
        val code = line.substringBefore("//").trim()
        val ignore = IGNORE_ANNOTATION.find(code)
        if (ignore != null && !ignore.groupValues[ARGUMENTS_GROUP].contains('"')) {
            return "@" + ignore.groupValues[1] + " without a written reason"
        }
        if (FOCUSED_TEST.containsMatchIn(code)) return "focused test is forbidden"
        return null
    }

    private companion object {
        const val ARGUMENTS_GROUP = 3
        // The optional dotted prefix catches fully qualified uses such as @kotlin.test.Ignore.
        val IGNORE_ANNOTATION = Regex("""@(?:[\w.]*\.)?(Ignore|Disabled)\b\s*(\((.*)\))?""")
        val FOCUSED_TEST = Regex("""\b(it|describe|test)\.only\b|\bfdescribe\b""")
        val SUPPRESS_ANNOTATION = Regex("""@(?:[\w.]*\.)?Suppress\s*\(""")
        val JUSTIFICATION = Regex("""(?://|/\*|\*)\s*\S""")
    }
}
