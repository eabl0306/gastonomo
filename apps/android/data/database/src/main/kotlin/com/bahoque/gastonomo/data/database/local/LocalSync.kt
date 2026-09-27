package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.data.database.GastonomoDatabase
import com.bahoque.gastonomo.data.database.dao.AccountDao
import com.bahoque.gastonomo.data.database.dao.CategoryDao
import com.bahoque.gastonomo.data.database.dao.LoanDao
import com.bahoque.gastonomo.data.database.dao.LoanPaymentDao
import com.bahoque.gastonomo.data.database.dao.MovementDao
import com.bahoque.gastonomo.data.database.dao.PendingWriteDao
import com.bahoque.gastonomo.data.database.dao.SyncDao
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import com.bahoque.gastonomo.data.database.entity.SettingsEntity
import com.bahoque.gastonomo.data.database.entity.SyncStateEntity

/**
 * Por donde va la sincronizacion, y donde acaba lo que llega.
 *
 * Todo se guarda por identificador (`upsert`), nunca reemplazando una tabla entera: un lote de
 * cambios es incremental, y borrar para insertar se llevaria por delante lo que no venia en el
 * lote.
 */
internal class LocalSync(database: GastonomoDatabase) : SyncStore {

    private val sync: SyncDao = database.sync()
    private val movements: MovementDao = database.movements()
    private val loans: LoanDao = database.loans()
    private val payments: LoanPaymentDao = database.loanPayments()
    private val categories: CategoryDao = database.categories()
    private val account: AccountDao = database.account()
    private val pending: PendingWriteDao = database.pending()

    override suspend fun cursor(): String? = sync.state()?.cursor

    override suspend fun save(batch: ChangeBatch) {
        if (batch.categories.isNotEmpty()) {
            categories.upsert(batch.categories.map(CategoryEntity::of))
        }
        if (batch.movements.isNotEmpty()) {
            movements.upsert(batch.movements.map(MovementEntity::of))
        }
        batch.loans.forEach { loan ->
            loans.upsert(LoanEntity.of(loan), loan.payments.map(LoanPaymentEntity::of), payments)
        }
        if (batch.loanPayments.isNotEmpty()) {
            payments.upsert(batch.loanPayments.map(LoanPaymentEntity::of))
        }
        batch.settings?.let { account.upsertSettings(SettingsEntity.of(it)) }
    }

    override suspend fun advance(cursor: String) {
        sync.save(state().copy(cursor = cursor))
    }

    override suspend fun pendingWrites(): Int = pending.count()

    override suspend fun startFullResync() {
        // La marca y el cursor primero, el borrado despues. Si la aplicacion muere entre las dos
        // cosas, al volver el cursor es null, que significa "bajalo todo": se arregla sola.
        sync.save(SyncStateEntity(cursor = null, fullResync = true))
        movements.deleteSynced()
        loans.deleteSynced()
        payments.deleteSyncedPayments()
        categories.clear()
        account.clearSettings()
    }

    override suspend fun finishFullResync() {
        sync.save(state().copy(fullResync = false))
    }

    override suspend fun fullResyncPending(): Boolean = sync.state()?.fullResync == true

    private suspend fun state(): SyncStateEntity =
        sync.state() ?: SyncStateEntity(cursor = null, fullResync = false)
}
