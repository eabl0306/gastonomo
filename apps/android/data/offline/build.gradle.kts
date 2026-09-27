plugins {
    id("gastonomo.android.library")
}

android {
    namespace = "com.bahoque.gastonomo.data.offline"
}

quality {
    lineCoverageMinimum.set(85)
}

dependencies {
    api(project(":core:application"))
    api(project(":data:database"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
