package sa.factory.qootsurplusfood129.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QootSurplusEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): QootSurplusDao
}
