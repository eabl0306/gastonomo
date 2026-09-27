package com.bahoque.gastonomo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind

/** Una categoria de la cuenta, con su conteo de uso tal como lo devuelve la API. */
@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "icon") val icon: String,
    @ColumnInfo(name = "color") val color: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "movement_count") val movementCount: Int,
) {
    fun toDomain(): Category = Category(
        id = CategoryId(id),
        name = name,
        kind = CategoryKind.fromWire(kind) ?: CategoryKind.BOTH,
        icon = icon,
        color = color,
        isDefault = isDefault,
        movementCount = movementCount,
    )

    companion object {
        fun of(category: Category): CategoryEntity = CategoryEntity(
            id = category.id.value,
            name = category.name,
            kind = category.kind.wire,
            icon = category.icon,
            color = category.color,
            isDefault = category.isDefault,
            movementCount = category.movementCount,
        )
    }
}
