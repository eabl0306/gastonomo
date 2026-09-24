plugins {
    id("gastonomo.jvm.library")
}

quality {
    lineCoverageMinimum.set(80)
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:common"))

}
