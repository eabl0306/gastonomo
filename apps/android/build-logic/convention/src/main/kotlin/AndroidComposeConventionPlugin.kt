import com.android.build.api.dsl.CommonExtension
import com.bahoque.gastonomo.gradle.bundle
import com.bahoque.gastonomo.gradle.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<CommonExtension> {
                buildFeatures.compose = true
            }
            val composeBom = library("androidx-compose-bom")
            dependencies {
                add("implementation", platform(composeBom))
                add("implementation", bundle("compose-ui"))
                add("implementation", library("androidx-activity-compose"))
                add("implementation", library("androidx-lifecycle-runtime-compose"))
                add("implementation", library("androidx-lifecycle-viewmodel-compose"))
                add("debugImplementation", bundle("compose-debug"))
                add("testImplementation", platform(composeBom))
                add("testImplementation", library("androidx-compose-ui-test-junit4"))
                add("androidTestImplementation", platform(composeBom))
            }
        }
    }
}
