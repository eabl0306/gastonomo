import com.bahoque.gastonomo.gradle.QualityExtension
import com.bahoque.gastonomo.gradle.bundle
import com.bahoque.gastonomo.gradle.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Todo lo que comparten los modulos de pantalla. Sin esto cada feature repetiria las
 * mismas nueve dependencias y bastaria que una se olvidara del sistema de diseno para
 * que su pantalla saliera con otros colores.
 *
 * Un modulo de feature solo tiene que declarar su namespace y, si necesita algo mas,
 * anadirlo encima.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("gastonomo.android.library")
            pluginManager.apply("gastonomo.android.compose")
            extensions.configure<QualityExtension> {
                lineCoverageMinimum.convention(FEATURE_LINE_COVERAGE)
            }
            dependencies {
                add("api", project(":core:application"))
                add("api", project(":core:designsystem"))
                add("api", project(":core:navigation"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:domain"))
                add("implementation", library("androidx-lifecycle-viewmodel-ktx"))
                add("implementation", library("androidx-navigation-compose"))
                add("testImplementation", project(":core:testing"))
                add("testImplementation", project(":core:screenshot"))
                add("testImplementation", bundle("screenshot-test"))
            }
        }
    }

    private companion object {
        /** Cobertura minima de lineas de un modulo de pantalla. */
        const val FEATURE_LINE_COVERAGE = 70
    }
}
