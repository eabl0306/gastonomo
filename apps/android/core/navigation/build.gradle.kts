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
    // Las pestanas se componen aqui, asi que hace falta el TabItem y las cadenas del sistema
    // de diseno. La arista va en este sentido y no al reves: el guardian del grafo pone la
    // navegacion por fuera del sistema de diseno, y el sistema de diseno no puede conocer las
    // rutas.
    api(project(":core:designsystem"))
    // Las rutas son tipos serializables: navigation-compose las necesita en el classpath
    // de quien las declara y de quien las consume.
    api(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // El guardian del grafo permite esta arista: :core:screenshot es soporte de test.
    testImplementation(project(":core:screenshot"))
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
