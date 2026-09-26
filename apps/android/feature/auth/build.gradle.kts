plugins {
    id("gastonomo.feature")
}

android {
    namespace = "com.bahoque.gastonomo.feature.auth"
}

dependencies {
    // El ViewModel se obtiene desde el composable de cada ruta, y el estado se recoge
    // atado al ciclo de vida para no seguir recomponiendo con la pantalla en segundo plano.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
