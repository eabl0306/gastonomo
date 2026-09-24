import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":core:common"))
    kover(project(":core:domain"))
    kover(project(":core:application"))
    kover(project(":data:health"))
    kover(project(":feature:health"))
    kover(project(":app"))
}

kover {
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
            rule("Aggregate line coverage of the Android app") {
                bound {
                    minValue.set(60)
                    coverageUnits.set(CoverageUnit.LINE)
                    aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
                }
            }
        }
        total {
            html { onCheck.set(false) }
            xml { onCheck.set(false) }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}
