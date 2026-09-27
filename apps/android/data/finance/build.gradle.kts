plugins {
    id("gastonomo.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bahoque.gastonomo.data.finance"
}

quality {
    lineCoverageMinimum.set(70)
}

dependencies {
    api(project(":core:application"))
    api(project(":data:network"))

    // Por el feed de cambios: trae categorias y ajustes en el mismo cuerpo, y sus DTO viven ahi.
    // Declararlos otra vez aqui dejaria dos mapeos del mismo tipo.
    implementation(project(":data:catalog"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
