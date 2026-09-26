plugins {
    id("gastonomo.jvm.library")
}

quality {
    // Modulo de dobles de test: su codigo se ejecuta desde los tests de los demas modulos,
    // no desde tests propios, asi que no tiene umbral de cobertura.
    lineCoverageMinimum.set(0)
}

dependencies {
    // Kotlin JVM puro, igual que el nucleo: asi lo pueden consumir tanto :core:application
    // como cualquier modulo Android. Un modulo Android aqui daria ambiguedad de variante al
    // consumirlo desde un modulo JVM.
    api(project(":core:application"))
    api(libs.bundles.jvm.unit.test)
}
