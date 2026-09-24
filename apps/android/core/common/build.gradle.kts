plugins {
    id("gastonomo.jvm.library")
}

quality {
    lineCoverageMinimum.set(80)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

}
