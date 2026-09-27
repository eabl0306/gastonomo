plugins {
    id("gastonomo.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bahoque.gastonomo.data.network"

    buildTypes {
        getByName("debug") {
            // 10.0.2.2 es el host de la maquina vista desde el emulador de Android.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("boolean", "HTTP_LOGGING", "true")
        }
        getByName("release") {
            // Produccion: la API detras del proxy de apps-01. Ver docs/despliegue.md.
            buildConfigField("String", "API_BASE_URL", "\"https://api.gastonomo.bahoque.com\"")
            buildConfigField("boolean", "HTTP_LOGGING", "false")
        }
    }
}

quality {
    lineCoverageMinimum.set(70)
}

dependencies {
    api(project(":core:application"))
    api(libs.bundles.ktor.client)
    api(project(":data:session"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
