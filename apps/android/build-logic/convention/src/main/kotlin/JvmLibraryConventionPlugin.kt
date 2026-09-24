import com.android.build.api.dsl.Lint
import com.bahoque.gastonomo.gradle.ProjectConfig
import com.bahoque.gastonomo.gradle.configureLintRules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            // Sin este plugin, lint trata los modulos JVM como dependencias externas y no
            // mira sus fuentes. Con el, el nucleo hexagonal tambien pasa por lint.
            pluginManager.apply("com.android.lint")
            pluginManager.apply("gastonomo.quality")
            pluginManager.apply("gastonomo.testing")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ProjectConfig.JAVA_VERSION
                targetCompatibility = ProjectConfig.JAVA_VERSION
            }
            extensions.configure<Lint> { configureLintRules() }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    allWarningsAsErrors.set(true)
                }
            }
        }
    }
}
