import com.bahoque.gastonomo.gradle.ModuleGraphGuardTask
import com.bahoque.gastonomo.gradle.ProjectConfig
import com.bahoque.gastonomo.gradle.QualityExtension
import com.bahoque.gastonomo.gradle.SourceGuardTask
import com.bahoque.gastonomo.gradle.library
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.Directory
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Enforcement de calidad: detekt con configuracion propia, Kover con umbrales que rompen
 * el build y el source guard para las reglas que detekt no cubre.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val quality = extensions.create<QualityExtension>("quality")
            quality.lineCoverageMinimum.convention(0)
            pluginManager.apply("io.gitlab.arturbosch.detekt")
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            configureDetekt()
            configureKover(quality)
            val guard = registerSourceGuard()
            val graphGuard = registerModuleGraphGuard()
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(guard)
                dependsOn(graphGuard)
                dependsOn("koverVerify")
            }
        }
    }
}

/**
 * detekt 1.23.8 embebe el compilador de Kotlin 2.0.21, cuyo parser de version de Java
 * rechaza "25". Se sustituye por un compilador embebido mas nuevo en el classpath de la
 * herramienta; el resto de la configuracion de detekt no cambia.
 */
private fun Project.configureDetekt() {
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        ignoreFailures = false
        parallel = true
        autoCorrect = false
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
        source.from("src/androidTest/kotlin", "src/androidTest/java")
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = ProjectConfig.JVM_TARGET
        clearJdkHome()
        reports {
            html.required.set(true)
            sarif.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
            md.required.set(false)
        }
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = ProjectConfig.JVM_TARGET
        jdkHome.convention(null as Directory?)
        jdkHome.set(null as Directory?)
    }
    dependencies {
        add("detektPlugins", library("detekt-formatting"))
    }
}

/**
 * detekt 1.23.8 embebe el compilador de Kotlin 2.0.21. Si se le pasa --jdk-home, ese
 * compilador monta el sistema de ficheros jrt y su parser rechaza "Java 25". Sin jdkHome
 * detekt analiza igual de bien: no usamos resolucion de tipos.
 */
private fun Detekt.clearJdkHome() {
    jdkHome.convention(null as Directory?)
    jdkHome.set(null as Directory?)
}

private fun Project.configureKover(quality: QualityExtension) {
    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    androidGeneratedClasses()
                    classes(
                        "*.BuildConfig",
                        "*.ComposableSingletons*",
                        "*.*_Factory",
                        "*.Hilt_*",
                        "hilt_aggregated_deps.*",
                        "dagger.hilt.*",
                    )
                    annotatedBy("androidx.compose.ui.tooling.preview.Preview")
                }
            }
            verify {
                rule("Line coverage floor for ${project.path}") {
                    bound {
                        minValue.set(quality.lineCoverageMinimum)
                        coverageUnits.set(CoverageUnit.LINE)
                        aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
                    }
                }
            }
        }
    }
}

private fun Project.registerSourceGuard(): TaskProvider<SourceGuardTask> =
    tasks.register<SourceGuardTask>("sourceGuard") {
        group = "verification"
        description = "Enforces file size limits and bans unjustified skipped or focused tests"
        sourceFiles.from(layout.projectDirectory.dir("src").asFileTree.matching { include("**/*.kt") })
        maxMainFileLines.set(ProjectConfig.MAX_MAIN_FILE_LINES)
        maxTestFileLines.set(ProjectConfig.MAX_TEST_FILE_LINES)
        projectDirectory.set(layout.projectDirectory.asFile.path)
        reportFile.set(layout.buildDirectory.file("reports/source-guard/source-guard.txt"))
    }

/**
 * Recoge las dependencias entre proyectos que declara el build script y se las pasa al
 * guardian. Se hace en afterEvaluate porque el bloque dependencies todavia no se ha leido
 * cuando se aplica el plugin.
 */
private fun Project.registerModuleGraphGuard(): TaskProvider<ModuleGraphGuardTask> {
    val ownPath = path
    val graphGuard = tasks.register<ModuleGraphGuardTask>("moduleGraphGuard") {
        group = "verification"
        description = "Enforces the direction of dependencies between Gradle modules"
        modulePath.set(ownPath)
        reportFile.set(layout.buildDirectory.file("reports/module-graph/module-graph.txt"))
    }
    afterEvaluate {
        val declared = configurations.flatMap { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
        }
        graphGuard.configure { dependencyPaths.set(declared.toSortedSet()) }
    }
    return graphGuard
}
