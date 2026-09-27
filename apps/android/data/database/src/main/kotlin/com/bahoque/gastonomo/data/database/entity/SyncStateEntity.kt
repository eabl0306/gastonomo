package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Por donde va la sincronizacion. Una sola fila.
 *
 * Vive en la misma base que los datos a proposito: si el cursor estuviera en otro fichero, los dos
 * podrian quedarse en versiones distintas y el dispositivo creeria tener algo que no tiene.
 *
 * @property cursor el cursor opaco del ultimo lote **ya escrito**. `null` es "nunca sincronizo".
 * @property fullResync true mientras una resincronizacion completa esta a medias. Sobrevive a que
 *   la aplicacion muera, que es justo para lo que esta: nadie confunde medio historico con el
 *   historico entero.
 */
@Entity(tableName = "sync_state")
internal data class SyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = ROW,
    @ColumnInfo(name = "cursor") val cursor: String?,
    @ColumnInfo(name = "full_resync") val fullResync: Boolean,
) {
    companion object {
        /** La fila. Con clave fija no puede haber dos estados a la vez. */
        const val ROW: Int = 1
    }
}
