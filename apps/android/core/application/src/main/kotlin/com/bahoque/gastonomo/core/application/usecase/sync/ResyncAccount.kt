package com.bahoque.gastonomo.core.application.usecase.sync

import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.core.application.state.ResyncResult
import com.bahoque.gastonomo.core.common.Outcome

/**
 * Borra lo local y lo reconstruye desde la API.
 *
 * Es la red de seguridad para cuando algo se tuerza. La idea es no necesitarla nunca, y por eso
 * esta enterrada en Ajustes y no a mano.
 */
class ResyncAccount(
    private val store: SyncStore,
    private val sync: SyncAccount,
) {

    suspend operator fun invoke(): ResyncResult {
        val pending = store.pendingWrites()
        if (pending > 0) return ResyncResult.HasPendingWrites(pending)
        // La marca entra antes del borrado: si la aplicacion muere aqui, al volver sigue puesta.
        store.startFullResync()
        return when (val result = sync()) {
            is Outcome.Success -> ResyncResult.Done
            is Outcome.Failure -> ResyncResult.Failed(result.error)
        }
    }
}
