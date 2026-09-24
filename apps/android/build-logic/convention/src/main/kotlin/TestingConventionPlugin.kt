import com.bahoque.gastonomo.gradle.bundle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", bundle("jvm-unit-test"))
            }
            tasks.withType<Test>().configureEach {
                // Robolectric instrumenta clases del JDK. En Java 25 hay que abrirle el paso.
                jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                )
                testLogging {
                    events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
                    exceptionFormat = TestExceptionFormat.FULL
                    showStackTraces = true
                }
            }
        }
    }
}
