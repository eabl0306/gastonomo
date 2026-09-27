plugins {
    id("gastonomo.android.library")
    alias(libs.plugins.kotlin.serialization)
}

// La API desplegada en apps-01, detras del proxy de Caddy. Ver docs/despliegue.md.
val deployedApiUrl = "https://api.gastonomo.bahoque.com"

// Debug apunta tambien a la desplegada, a proposito.
//
// Antes iba a http://10.0.2.2:8080, que es el alias del host de la maquina visto DESDE EL
// EMULADOR. En un telefono real esa direccion no existe, asi que la aplicacion decia "no hay
// conexion" sin mas pista, y el motivo no estaba a la vista en ningun sitio.
//
// Para desarrollar contra una API local no hace falta tocar este fichero:
//
//   ./gradlew assembleDebug -PgastonomoApiUrl=http://10.0.2.2:8080     (emulador)
//   ./gradlew assembleDebug -PgastonomoApiUrl=http://192.168.1.50:8080 (telefono en la LAN)
//
// o dejandolo fijo en ~/.gradle/gradle.properties. El texto claro sigue permitido solo en
// debug y solo para hosts locales, asi que ese override funciona sin tocar nada mas.
//
// OJO con la consecuencia: por defecto, un build de debug escribe en la base de datos de
// PRODUCCION. Mientras la aplicacion no tenga usuarios de verdad da igual; cuando los tenga,
// esto tiene que volver a apuntar a un entorno propio.
val debugApiUrl: String = providers.gradleProperty("gastonomoApiUrl").getOrElse(deployedApiUrl)

android {
    namespace = "com.bahoque.gastonomo.data.network"

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
            buildConfigField("boolean", "HTTP_LOGGING", "true")
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"$deployedApiUrl\"")
            buildConfigField("boolean", "HTTP_LOGGING", "false")
        }
    }
}

quality {
    lineCoverageMinimum.set(70)
}

dependencies {
    api(project(":core:application"))
    api(libs.bundles.ktor.client)
    api(project(":data:session"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
}
