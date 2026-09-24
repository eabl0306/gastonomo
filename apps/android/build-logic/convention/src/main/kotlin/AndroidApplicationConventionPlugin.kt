import com.android.build.api.dsl.ApplicationExtension
import com.bahoque.gastonomo.gradle.ProjectConfig
import com.bahoque.gastonomo.gradle.configureAndroidCommon
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("gastonomo.quality")
            pluginManager.apply("gastonomo.testing")
            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                defaultConfig.applicationId = ProjectConfig.APPLICATION_ID
                defaultConfig.targetSdk = ProjectConfig.TARGET_SDK
                defaultConfig.versionCode = ProjectConfig.VERSION_CODE
                defaultConfig.versionName = ProjectConfig.VERSION_NAME
                defaultConfig.testInstrumentationRunner = ProjectConfig.TEST_RUNNER
            }
        }
    }
}
