plugins {
    id("gastonomo.android.application")
    id("gastonomo.android.compose")
}

android {
    namespace = "com.bahoque.gastonomo"

    // El idioma se elige dentro de la aplicacion, en Ajustes, asi que el bundle no puede
    // repartirse por idioma: si se reparte, el telefono solo instala el del sistema y elegir
    // el otro no cambia ni un texto.
    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        getByName("release") {
            optimization {
                enable = false
            }
        }
    }
}

quality {
    lineCoverageMinimum.set(60)
}

dependencies {
    implementation(project(":core:application"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))

    implementation(project(":data:auth"))
    implementation(project(":data:cache"))
    implementation(project(":data:catalog"))
    implementation(project(":data:finance"))
    implementation(project(":data:network"))
    implementation(project(":data:session"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:export"))
    implementation(project(":feature:home"))
    implementation(project(":feature:loans"))
    implementation(project(":feature:movements"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.konsist)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)

    androidTestImplementation(libs.bundles.android.instrumented.test)
}
