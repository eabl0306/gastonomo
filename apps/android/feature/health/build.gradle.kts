plugins {
    id("gastonomo.android.library")
    id("gastonomo.android.compose")
}

android {
    namespace = "com.bahoque.gastonomo.feature.health"
}

quality {
    lineCoverageMinimum.set(70)
}

dependencies {
    api(project(":core:application"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
