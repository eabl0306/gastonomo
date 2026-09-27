package com.bahoque.gastonomo.core.application.usecase.sync

import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.application.port.PendingWritesGateway
import kotlinx.coroutines.flow.Flow

/** Lo que queda por subir. Ajustes lo usa para decir la verdad sobre la sincronizacion. */
class ObservePendingWrites(private val gateway: PendingWritesGateway) {
    operator fun invoke(): Flow<List<PendingWrite>> = gateway.observe()
}

/**
 * Corrige lo que la API rechazo. La clave de idempotencia se conserva: un 422 no la quema (ADR
 * 0012), asi que reenviar con la misma es lo correcto y evita duplicar.
 */
class CorrectPendingWrite(private val gateway: PendingWritesGateway) {
    suspend operator fun invoke(id: String, draft: PendingDraft) = gateway.correct(id, draft)
}

/**
 * Tira una escritura que no se puede arreglar, y con ella su registro provisional.
 *
 * Borra dinero que el usuario habia registrado, asi que la pantalla pregunta antes.
 */
class DiscardPendingWrite(private val gateway: PendingWritesGateway) {
    suspend operator fun invoke(id: String) = gateway.discard(id)
}

/** Pide otra tanda de subida ahora, sin esperar a que el sistema decida. */
class RetryPendingWrites(private val gateway: PendingWritesGateway) {
    suspend operator fun invoke() = gateway.retry()
}
