package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.application.port.ChangeGateway
import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.core.common.Outcome

/**
 * El feed de cambios programado por lotes.
 *
 * Devuelve los lotes en orden y apunta con que cursor se pidio cada uno, que es lo unico con lo que
 * se puede comprobar que el cliente no avanza antes de escribir.
 */
class FakeChangeGateway(
    private val batches: List<ChangeBatch> = emptyList(),
    private val failAfter: Int = Int.MAX_VALUE,
) : ChangeGateway {

    /** Los cursores con los que se pidio cada lote, en orden. */
    val asked = mutableListOf<String?>()

    private var served = 0

    override suspend fun changes(since: String?, limit: Int?): Outcome<ChangeBatch, ApiError> {
        asked += since
        if (served >= failAfter) return Outcome.Failure(ApiError.Network)
        // Agotados los lotes programados, la cuenta esta al dia: el cursor se queda donde estaba.
        val batch = batches.getOrNull(served)?.also { served++ }
            ?: ChangeBatch(cursor = since ?: "", hasMore = false)
        return Outcome.Success(batch)
    }
}

/**
 * El estado de sincronizacion en memoria.
 *
 * Apunta el **orden** de lo que le pasa, porque la regla que hay que probar es de orden: se escribe
 * el lote y solo entonces se avanza el cursor.
 */
class FakeSyncStore(
    private var cursor: String? = null,
    private var pending: Int = 0,
    private var resyncing: Boolean = false,
) : SyncStore {

    /** Lo que se le pidio, en orden: `save:<cursor>`, `advance:<cursor>`, `start`, `finish`. */
    val log = mutableListOf<String>()

    /** Los lotes guardados. */
    val saved = mutableListOf<ChangeBatch>()

    /** Si [save] tiene que fallar, para comprobar que el cursor no avanza detras de un fallo. */
    var failOnSave: Boolean = false

    override suspend fun cursor(): String? = cursor

    override suspend fun save(batch: ChangeBatch) {
        log += "save:${batch.cursor}"
        if (failOnSave) error("la escritura local fallo")
        saved += batch
    }

    override suspend fun advance(cursor: String) {
        log += "advance:$cursor"
        this.cursor = cursor
    }

    override suspend fun pendingWrites(): Int = pending

    override suspend fun startFullResync() {
        log += "start"
        resyncing = true
        cursor = null
        saved.clear()
    }

    override suspend fun finishFullResync() {
        log += "finish"
        resyncing = false
    }

    override suspend fun fullResyncPending(): Boolean = resyncing
}
