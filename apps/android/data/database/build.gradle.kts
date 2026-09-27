plugins {
    id("gastonomo.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bahoque.gastonomo.data.database"
}

room {
    // El esquema exportado se versiona en el repositorio. Sin el, la primera actualizacion que
    // cambie una tabla no tiene desde donde migrar y borra los datos de quien ya la tenia.
    schemaDirectory("$projectDir/schemas")
}

quality {
    lineCoverageMinimum.set(80)
    // Room genera un *_Impl por DAO y otro por base de datos. Contarlos mide al generador, no a
    // este modulo, igual que el proyecto ya excluye lo que generan Hilt y Dagger. Lo que si se
    // mide son las entidades, los conversores y el mapeo, que es donde se rompe el dinero.
    untestableOnJvm.addAll(
        "com.bahoque.gastonomo.data.database.GastonomoDatabase_Impl",
        "com.bahoque.gastonomo.data.database.GastonomoDatabase_Impl*",
        "com.bahoque.gastonomo.data.database.dao.*_Impl",
        "com.bahoque.gastonomo.data.database.dao.*_Impl*",
    )
}

dependencies {
    api(project(":core:application"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
