package com.cawfee.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Espresso shot record — a representative Room port of one of the Swift @Model types
 * (Shot.swift). The same pattern (entity + DAO) extends to the remaining models
 * (Recipe, BeanProfile, TastingNote, etc.) listed in docs/ANDROID_PORT.md.
 */
@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val beanName: String?,
    val drink: String,
    val doseGrams: Double,
    val yieldGrams: Double,
    val preInfusionSeconds: Double,
    val totalSeconds: Double,
    val rating: Int,
    val notes: String,
) {
    val ratio: Double get() = if (doseGrams > 0) yieldGrams / doseGrams else 0.0
}

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<ShotEntity>>

    @Insert
    suspend fun insert(shot: ShotEntity): Long

    @Delete
    suspend fun delete(shot: ShotEntity)
}

@Database(entities = [ShotEntity::class], version = 1, exportSchema = true)
abstract class CawfeeDatabase : RoomDatabase() {
    abstract fun shotDao(): ShotDao
}
