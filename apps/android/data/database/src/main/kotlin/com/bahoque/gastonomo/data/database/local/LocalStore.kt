package com.bahoque.gastonomo.data.database.local

import android.content.Context
import com.bahoque.gastonomo.core.common.DefaultDispatcherProvider
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.data.database.GastonomoDatabase
import kotlinx.coroutines.withContext

/**
 * Todo lo que guarda el dispositivo, agrupado.
 *
 * Es lo unico que el resto de la aplicacion ve de este modulo: nadie fuera de aqui conoce Room, ni
 * una entidad, ni un DAO. Igual que `:data:network` no deja salir un DTO de Ktor.
 *
 * Vive tanto como el proceso, igual que la raiz de composicion que lo construye.
 */
class LocalStore internal constructor(
    private val database: GastonomoDatabase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) {

    val movements: LocalMovements = LocalMovements(database.movements())
    val loans: LocalLoans = LocalLoans(database.loans(), database.loanPayments())
    val categories: LocalCategories = LocalCategories(database.categories())
    val account: LocalAccount = LocalAccount(database.account())
    val currencies: LocalCurrencies = LocalCurrencies(database.currencies())
    val pending: LocalPendingWrites = LocalPendingWrites(database.pending())

    /**
     * Vacia todo, porque cambio la cuenta. Sin esto, quien entra despues de que otro cierre sesion
     * en el mismo telefono veria los movimientos del anterior, y ademas en disco.
     */
    suspend fun clear() {
        // `clearAllTables` es bloqueante, y Room se niega a correr en el hilo principal. Cerrar
        // sesion se llama desde el scope del ViewModel, que es Main: sin este cambio de hilo, la
        // aplicacion se cae al salir. Los DAO no lo necesitan, que se despachan solos.
        withContext(dispatchers.io) { database.clearAllTables() }
    }

    /** Cierra el fichero. En produccion no se llama: la base vive lo que vive el proceso. */
    fun close() {
        database.close()
    }
}

/** La fabrica que llama el contenedor de la aplicacion. */
fun createLocalStore(context: Context): LocalStore =
    LocalStore(GastonomoDatabase.open(context))

/**
 * Una base en memoria, que no deja fichero.
 *
 * Esta en el codigo de produccion y no en un modulo de test porque es la unica forma de que otro
 * modulo monte un almacen de verdad sin ver Room, que es justamente lo que este modulo no deja
 * salir. La usan los tests de `:data:offline`.
 */
fun createInMemoryLocalStore(context: Context): LocalStore =
    LocalStore(GastonomoDatabase.inMemory(context))
