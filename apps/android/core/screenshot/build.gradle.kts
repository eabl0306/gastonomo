plugins {
    id("gastonomo.android.library")
    id("gastonomo.android.compose")
}

android {
    namespace = "com.bahoque.gastonomo.core.screenshot"
}

quality {
    // Utilidad de captura: la ejercitan los tests de los modulos que la usan.
    lineCoverageMinimum.set(0)
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:navigation"))
    api(project(":core:testing"))
    api(libs.bundles.screenshot.test)
    // El source set principal de este modulo es codigo de test, asi que las bibliotecas de
    // test van en api: quien lo consume lo hace desde su propio source set de test.
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.compose.ui.test.manifest)
}
