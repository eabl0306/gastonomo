package com.bahoque.gastonomo.core.application.port

import kotlinx.coroutines.flow.Flow

/**
 * La cola de lo que se registro sin conexion y todavia no ha llegado a la API.
 *
 * Existe como puerto porque hay una pantalla que la ensena: sin ella, una escritura que la API
 * rechaza desapareceria en silencio y el usuario nunca sabria que ese gasto no esta.
 */
interface PendingWritesGateway {

    /** Lo que queda por subir, en el orden en el que se registro. */
    fun observe(): Flow<List<PendingWrite>>

    /**
     * Cambia el borrador de una escritura que la API rechazo, **conservando su clave de
     * idempotencia**: un 422 no la quema, asi que reintentar con la misma es lo correcto y ademas
     * lo unico que garantiza que no se duplique.
     */
    suspend fun correct(id: String, draft: PendingDraft)

    /**
     * La tira, y con ella el registro provisional que la acompanaba.
     *
     * Es la unica salida cuando la API rechaza algo que no se puede arreglar. Borra dinero que el
     * usuario habia registrado, asi que quien la llame tiene que preguntar antes.
     */
    suspend fun discard(id: String)

    /** Pide otra tanda de subida ahora mismo, sin esperar a que el sistema decida. */
    suspend fun retry()
}
