package com.davesspot.drinklog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drink_types ORDER BY displayOrder ASC, id DESC")
    fun getAllDrinkTypes(): Flow<List<DrinkType>>

    @Insert
    suspend fun insertDrinkType(drinkType: DrinkType)

    @Insert
    suspend fun insertConsumption(log: ConsumptionLog)

    @Query("SELECT * FROM consumption_logs WHERE timestamp BETWEEN :start AND :end")
    fun getLogsInRange(start: Long, end: Long): Flow<List<ConsumptionLog>>

    @Query("DELETE FROM consumption_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)

    @Delete
    suspend fun deleteConsumption(log: ConsumptionLog)

    @Delete
    suspend fun deleteDrinkType(drinkType: DrinkType)

    @Update
    suspend fun updateConsumption(log: ConsumptionLog)

    @Update
    suspend fun updateDrinkType(drinkType: DrinkType)
}
