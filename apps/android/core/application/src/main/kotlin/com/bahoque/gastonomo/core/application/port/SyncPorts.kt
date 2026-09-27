package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Un lote de `GET /v1/changes`.
 *
 * Como el historico es de solo anadir, esto es casi siempre "cosas creadas despues de la ultima
 * vez". Lo unico que muta son las categorias (renombrar, archivar y recuperar) y los ajustes.
 *
 * Un abono puede llegar dos veces, dentro de su prestamo y en [loanPayments]. Todo se guarda por
 * identificador, asi que guardarlo dos veces deja lo mismo que guardarlo una.
 *
 * @property cursor hasta donde llega el lote. **Se guarda despues de escribirlo**, nunca antes.
 * @property hasMore true si quedan lotes detras.
 */
data class ChangeBatch(
    val cursor: String,
    val hasMore: Boolean,
    val movements: List<Movement> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val loanPayments: List<LoanPayment> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: UserSettings? = null,
)

/** Lo que ha cambiado en la cuenta desde el cursor que tenga guardado el dispositivo. */
interface ChangeGateway {

    /**
     * `GET /v1/changes`.
     *
     * @param since el cursor del ultimo lote que se guardo. Con `null` trae desde el principio,
     *   que es el arranque en frio de un dispositivo nuevo. **El cursor es opaco**: se guarda y se
     *   devuelve tal cual, sin interpretarlo.
     */
    suspend fun changes(since: String?, limit: Int? = null): Outcome<ChangeBatch, ApiError>
}

/**
 * Donde el dispositivo guarda lo que le llega y por donde va.
 *
 * El cursor y los datos viven en la misma base, asi que no pueden quedarse en versiones
 * distintas de dos ficheros.
 */
interface SyncStore {

    /** Por donde iba, o `null` si nunca sincronizo. */
    suspend fun cursor(): String?

    /** Escribe el lote. Todo por identificador: repetir un lote no duplica nada. */
    suspend fun save(batch: ChangeBatch)

    /**
     * Avanza el cursor.
     *
     * Se llama **despues** de [save] y nunca antes: si la escritura falla y el cursor ya subio,
     * ese lote no lo vuelve a mandar nadie.
     */
    suspend fun advance(cursor: String)

    /** Escrituras que el dispositivo todavia no ha subido. */
    suspend fun pendingWrites(): Int

    /**
     * Marca que empieza una resincronizacion completa y borra lo local.
     *
     * La marca se pone **antes** de borrar. Si la aplicacion muere a la mitad, al volver sigue
     * puesta, asi que nadie confunde medio historico con el historico entero.
     */
    suspend fun startFullResync()

    /** Quita la marca: lo local vuelve a estar completo. */
    suspend fun finishFullResync()

    /** True si quedo una resincronizacion a medias. */
    suspend fun fullResyncPending(): Boolean
}
