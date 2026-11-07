package sa.factory.qootsurplusfood129.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QootSurplusDao {
    @Query("SELECT * FROM food_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<QootSurplusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QootSurplusEntity>)

    @Query("DELETE FROM food_items_cache")
    suspend fun clearAll()
}
