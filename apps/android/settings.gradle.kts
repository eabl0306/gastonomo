pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Gastonomo"

include(":app")

// Nucleo hexagonal: Kotlin JVM puro, sin SDK de Android en el classpath.
include(":core:common")
include(":core:domain")
include(":core:application")

// Compartido por la capa de presentacion. No lo puede usar :data.
include(":core:designsystem")
include(":core:navigation")

// Utilidades de test compartidas. Solo las consumen los source sets de test, nunca el
// codigo de produccion. :core:testing es JVM puro para que tambien lo pueda usar el
// nucleo; :core:screenshot es Android porque renderiza composables.
include(":core:testing")
include(":core:screenshot")

// Adaptadores driven: hablan con la API y con el almacenamiento del dispositivo.
include(":data:network")
include(":data:session")
include(":data:auth")
include(":data:finance")
include(":data:catalog")

// Adaptadores driving: una pantalla o un grupo de pantallas cada uno.
include(":feature:auth")
include(":feature:home")
include(":feature:movements")
include(":feature:loans")
include(":feature:settings")
include(":feature:categories")
include(":feature:export")
