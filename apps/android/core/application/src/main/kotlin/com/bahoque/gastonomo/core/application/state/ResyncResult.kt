package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.application.error.ApiError

/** Como acabo una resincronizacion completa. */
sealed interface ResyncResult {

    /** Lo local se borro y se reconstruyo entero. */
    data object Done : ResyncResult

    /**
     * Habia escrituras sin subir y **no se ha borrado nada**.
     *
     * Borrar lo local con cosas sin subir es perder movimientos del usuario, que es el peor fallo
     * posible de esta aplicacion. Primero se suben.
     */
    data class HasPendingWrites(val count: Int) : ResyncResult

    /**
     * Se corto por la red. Lo local se quedo a medias **y marcado como tal**, asi que el siguiente
     * arranque la termina en vez de creer que esta completo.
     */
    data class Failed(val error: ApiError) : ResyncResult
}
