package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una escritura que el usuario hizo sin conexion y que todavia no ha llegado a la API.
 *
 * La fila guarda tres cosas que no se pueden perder:
 *
 * - **La clave de idempotencia**, generada al encolar y **nunca regenerada**. Si se regenerara, un
 *   reintento despues de un timeout que en realidad si llego crearia el gasto dos veces, y no hay
 *   pantalla para borrarlo. Ver el ADR 0012.
 * - **El orden**, en [queuedAt]: se sube en el orden en que el usuario registro.
 * - **De que depende**, en [dependsOn]: un abono no se puede subir antes que el prestamo al que
 *   pertenece, porque en el servidor ese prestamo todavia no existe.
 *
 * @property id el mismo identificador local con el que se guardo el registro provisional, para
 *   poder sustituirlo por el de verdad cuando la API conteste.
 * @property payload el borrador serializado. Va en JSON y no en columnas porque los tres tipos de
 *   escritura tienen campos distintos y una tabla con la union de todos seria mentira en cada fila.
 * @property failure el motivo del ultimo fallo **permanente**. Mientras es null, la fila se
 *   reintenta; en cuanto tiene algo, deja de reintentarse y hay que corregirla a mano.
 */
@Entity(
    tableName = "pending_writes",
    indices = [Index(value = ["queued_at"])],
)
internal data class PendingWriteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "payload") val payload: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "loan_id") val loanId: String?,
    @ColumnInfo(name = "depends_on") val dependsOn: String?,
    @ColumnInfo(name = "queued_at") val queuedAt: Long,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "failure") val failure: String? = null,
)
