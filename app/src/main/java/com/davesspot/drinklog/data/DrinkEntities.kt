package com.davesspot.drinklog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drink_types")
data class DrinkType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val standardUnits: Double,
    val displayOrder: Int = 0
)

@Entity(tableName = "consumption_logs")
data class ConsumptionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val standardUnits: Double,
    val timestamp: Long
)
