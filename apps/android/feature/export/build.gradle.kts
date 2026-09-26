plugins {
    id("gastonomo.feature")
}

android {
    namespace = "com.bahoque.gastonomo.feature.export"
}

dependencies {
    // FileProvider: el archivo exportado sale por la hoja de compartir del sistema, y eso
    // exige una uri de contenido, no una ruta.
    implementation(libs.androidx.core.ktx)
}
