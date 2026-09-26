package com.bahoque.gastonomo.data.network

/**
 * Donde esta la API y si se registra el trafico.
 *
 * En debug apunta a `http://10.0.2.2:8080`, que es el host de la maquina visto desde el
 * emulador. El registro de peticiones solo se enciende en debug: en release imprimiria
 * tokens en el log.
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
