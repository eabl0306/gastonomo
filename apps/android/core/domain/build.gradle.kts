plugins {
    id("gastonomo.jvm.library")
}

quality {
    // El nucleo se prueba al 80 por ciento de lineas como minimo.
    lineCoverageMinimum.set(80)
}
