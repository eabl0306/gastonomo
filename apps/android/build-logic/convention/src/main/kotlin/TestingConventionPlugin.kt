import com.bahoque.gastonomo.gradle.bundle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

private const val ROBORAZZI_OUTPUT = "outputs/roborazzi"
private const val TEST_HEAP = "2g"

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", bundle("jvm-unit-test"))
            }
            tasks.withType<Test>().configureEach {
                // El worker se queda con 512 MB si no se le dice otra cosa, y Robolectric con
                // Compose no cabe: el proceso muere y Gradle lo cuenta como
                // java.io.EOFException, sin escribir ni un XML de resultados. Parece un test
                // roto y es falta de memoria.
                maxHeapSize = TEST_HEAP
                // Robolectric instrumenta clases del JDK. En Java 25 hay que abrirle el paso.
                jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                )
                // Roborazzi solo escribe la imagen cuando esta en modo grabacion. Sin esto
                // captureRoboImage no hace nada y el test pasa sin haber capturado nada.
                systemProperty("roborazzi.test.record", "true")
                // Las capturas son una salida de la tarea. Sin declararlas, un acierto de
                // cache deja el test en verde y el directorio de imagenes vacio.
                outputs.dir(layout.buildDirectory.dir(ROBORAZZI_OUTPUT))
                    .withPropertyName("roborazziScreenshots")
                testLogging {
                    events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
                    exceptionFormat = TestExceptionFormat.FULL
                    showStackTraces = true
                }
            }
        }
    }
}
