plugins {
    id("gastonomo.jvm.library")
}

quality {
    lineCoverageMinimum.set(80)
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:common"))

    // Los dobles de los puertos viven en :core:testing para no viajar en el APK. La
    // dependencia solo existe en el source set de test, asi que no hay ciclo de produccion.
    testImplementation(project(":core:testing"))
}
