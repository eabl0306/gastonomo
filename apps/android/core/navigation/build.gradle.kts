plugins {
    id("gastonomo.android.library")
    // navigation-compose declara sus destinos con lambdas @Composable, asi que este modulo
    // necesita el compilador de Compose aunque no pinte nada.
    id("gastonomo.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bahoque.gastonomo.core.navigation"
}

quality {
    lineCoverageMinimum.set(70)
}

dependencies {
    // Las rutas son tipos serializables: navigation-compose las necesita en el classpath
    // de quien las declara y de quien las consume.
    api(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
