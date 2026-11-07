package sa.factory.qootsurplusfood129.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.qootsurplusfood129.model.QootSurplusItem

@Entity(tableName = "food_items_cache")
data class QootSurplusEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): QootSurplusItem = QootSurplusItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
