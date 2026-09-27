package com.bahoque.gastonomo.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bahoque.gastonomo.data.database.entity.SyncStateEntity

/** La fila del estado de sincronizacion. */
@Dao
internal interface SyncDao {

    @Query("SELECT * FROM sync_state WHERE id = :id")
    suspend fun state(id: Int = SyncStateEntity.ROW): SyncStateEntity?

    @Upsert
    suspend fun save(state: SyncStateEntity)
}
