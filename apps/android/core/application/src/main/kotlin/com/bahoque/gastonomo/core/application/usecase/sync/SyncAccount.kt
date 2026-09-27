package com.bahoque.gastonomo.core.application.usecase.sync

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.ChangeGateway
import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.core.common.Outcome

/**
 * Pone el dispositivo al dia con lo que haya pasado en otros.
 *
 * Pide lotes desde el cursor guardado hasta que no queda nada. **Escribe el lote y solo entonces
 * avanza el cursor**: al contrario, un fallo al escribir perderia ese lote para siempre, porque
 * nadie lo vuelve a mandar.
 *
 * Es seguro llamarlo en cualquier momento y es seguro que se corte a la mitad. Cada lote que se
 * guarda deja el dispositivo con menos que bajar, y un lote repetido no duplica nada porque todo
 * se guarda por identificador.
 *
 * Si habia una resincronizacion completa a medias, terminarla es esto mismo: seguir pidiendo desde
 * donde se quedo. Solo al acabar se quita la marca.
 */
class SyncAccount(
    private val gateway: ChangeGateway,
    private val store: SyncStore,
) {

    suspend operator fun invoke(): Outcome<Int, ApiError> {
        var batches = 0
        while (batches < MAX_BATCHES) {
            val result = gateway.changes(store.cursor())
            if (result is Outcome.Failure) return result
            val batch = (result as Outcome.Success).value
            store.save(batch)
            store.advance(batch.cursor)
            batches++
            if (!batch.hasMore) break
        }
        if (store.fullResyncPending()) store.finishFullResync()
        return Outcome.Success(batches)
    }

    private companion object {
        /**
         * Tope de lotes por pasada.
         *
         * Con el limite por defecto de la API son 200.000 cambios, mas de lo que una cuenta
         * personal acumula en anos. Esta para que un `hasMore` que nunca baje no deje la
         * aplicacion dando vueltas.
         */
        const val MAX_BATCHES = 1_000
    }
}
