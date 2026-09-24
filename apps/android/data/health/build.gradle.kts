plugins {
    id("gastonomo.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bahoque.gastonomo.data.health"

    buildTypes {
        getByName("debug") {
            // 10.0.2.2 es el host de la maquina vista desde el emulador de Android.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
        }
        getByName("release") {
            // Placeholder: se sustituye por el host real cuando exista despliegue.
            buildConfigField("String", "API_BASE_URL", "\"https://api.gastonomo.invalid\"")
        }
    }
}

quality {
    lineCoverageMinimum.set(80)
}

dependencies {
    api(project(":core:application"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.ktor.client.mock)
}
