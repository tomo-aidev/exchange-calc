package com.exchangecalc.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ConversionHistory>>

    @Insert
    suspend fun insert(history: ConversionHistory)

    @Delete
    suspend fun delete(history: ConversionHistory)

    @Query("DELETE FROM conversion_history")
    suspend fun deleteAll()
}
