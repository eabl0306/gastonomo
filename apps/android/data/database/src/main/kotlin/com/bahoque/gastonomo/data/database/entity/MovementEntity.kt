package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/**
 * Un ingreso o un gasto.
 *
 * **La categoria va embebida, no referenciada.** Es como la manda la API, que entrega cada
 * movimiento con su categoria completa, y tiene dos ventajas aqui: la fila se pinta sin cruzar
 * nada, y borrar una categoria no deja movimientos sin tile que ensenar. El precio es que
 * renombrar una categoria no toca los movimientos ya guardados hasta que se vuelvan a bajar.
 *
 * El indice es por fecha descendente porque es el unico orden en el que se leen: la lista de
 * Movimientos va del dia mas reciente al mas antiguo, y el periodo se filtra por rango.
 */
@Entity(
    tableName = "movements",
    indices = [Index(value = ["occurred_on"], orders = [Index.Order.DESC])],
)
internal data class MovementEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "occurred_on") val occurredOn: LocalDate,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "title") val title: String,
    @Embedded val money: MoneyColumns,
    @Embedded(prefix = "category_") val category: CategoryEntity,
    @ColumnInfo(name = "loan_id") val loanId: String?,
    @ColumnInfo(name = "loan_payment_id") val loanPaymentId: String?,
) {
    fun toDomain(): Movement = Movement(
        id = MovementId(id),
        occurredOn = occurredOn,
        kind = MovementKind.fromWire(kind) ?: MovementKind.OUT,
        title = title,
        money = money.toDomain(),
        category = category.toDomain(),
        loanId = loanId,
        loanPaymentId = loanPaymentId,
    )

    companion object {
        fun of(movement: Movement): MovementEntity = MovementEntity(
            id = movement.id.value,
            occurredOn = movement.occurredOn,
            kind = movement.kind.wire,
            title = movement.title,
            money = MoneyColumns.of(movement.money),
            category = CategoryEntity.of(movement.category),
            loanId = movement.loanId,
            loanPaymentId = movement.loanPaymentId,
        )
    }
}
