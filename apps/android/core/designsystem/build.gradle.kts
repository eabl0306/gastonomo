plugins {
    id("gastonomo.android.library")
    id("gastonomo.android.compose")
}

android {
    namespace = "com.bahoque.gastonomo.core.designsystem"
}

quality {
    // El formato de dinero y de fechas vive aqui y es donde mas silenciosamente se rompen
    // las cosas, asi que este modulo se prueba al nivel del nucleo.
    lineCoverageMinimum.set(80)
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.androidx.compose.foundation)

    // La utilidad de captura vive en :core:screenshot, que a su vez usa este modulo para
    // envolver el composable en el tema. Es una dependencia solo del source set de test,
    // asi que no hay ciclo en el grafo de produccion.
    testImplementation(project(":core:screenshot"))
    testImplementation(libs.bundles.screenshot.test)
}
