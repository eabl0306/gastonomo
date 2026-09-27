package com.bahoque.gastonomo.data.network

/**
 * Donde esta la API y si se registra el trafico.
 *
 * Los dos buildType apuntan por defecto a la API desplegada,
 * `https://api.gastonomo.bahoque.com`. Debug se puede redirigir a una API local sin tocar
 * codigo, con `-PgastonomoApiUrl=...`; el `build.gradle.kts` de este modulo lo explica.
 *
 * El registro de peticiones solo se enciende en debug: en release imprimiria tokens en el
 * log. Como debug habla con produccion, ese log lleva tokens de produccion, asi que no se
 * comparte a la ligera.
 */
data class ApiConfig(
    val baseUrl: String = BuildConfig.API_BASE_URL,
    val loggingEnabled: Boolean = BuildConfig.HTTP_LOGGING,
) {
    companion object {
        /** Prefijo de version de todas las rutas de negocio. */
        const val VERSION_PATH = "v1"
    }
}
