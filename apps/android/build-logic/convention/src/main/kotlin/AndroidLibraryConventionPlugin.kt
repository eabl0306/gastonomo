import com.android.build.api.dsl.LibraryExtension
import com.bahoque.gastonomo.gradle.ProjectConfig
import com.bahoque.gastonomo.gradle.configureAndroidCommon
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("gastonomo.quality")
            pluginManager.apply("gastonomo.testing")
            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                defaultConfig.testInstrumentationRunner = ProjectConfig.TEST_RUNNER
                testOptions.targetSdk = ProjectConfig.TARGET_SDK
                lint.targetSdk = ProjectConfig.TARGET_SDK
            }
        }
    }
}
