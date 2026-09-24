plugins {
    id("gastonomo.android.application")
    id("gastonomo.android.compose")
}

android {
    namespace = "com.bahoque.gastonomo"

    buildTypes {
        getByName("release") {
            optimization {
                enable = false
            }
        }
    }
}

quality {
    lineCoverageMinimum.set(60)
}

dependencies {
    implementation(project(":core:application"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":data:health"))
    implementation(project(":feature:health"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.konsist)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)

    androidTestImplementation(libs.bundles.android.instrumented.test)
}
