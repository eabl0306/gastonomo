plugins {
    id("gastonomo.jvm.library")
}

quality {
    lineCoverageMinimum.set(85)
}

dependencies {
    // Kotlin JVM puro: decora puertos, no conoce Ktor ni el SDK de Android. Se puede probar
    // entero sin Robolectric.
    api(project(":core:application"))

    testImplementation(project(":core:testing"))
}
