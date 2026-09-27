package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.application.port.PendingWritesGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** La cola de escrituras sin subir, programable. Por defecto esta vacia: todo subido. */
class FakePendingWritesGateway(writes: List<PendingWrite> = emptyList()) : PendingWritesGateway {

    private val mutable = MutableStateFlow(writes)

    /** Cuantas veces se pidio reintentar a mano. */
    var retries: Int = 0
        private set

    /** Lo que se corrigio, por identificador. */
    val corrected = mutableMapOf<String, PendingDraft>()

    override fun observe(): Flow<List<PendingWrite>> = mutable.asStateFlow()

    override suspend fun correct(id: String, draft: PendingDraft) {
        corrected[id] = draft
        mutable.update { list ->
            list.map { if (it.id == id) it.copy(draft = draft, failure = null) else it }
        }
    }

    override suspend fun discard(id: String) {
        mutable.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun retry() {
        retries++
    }
}
