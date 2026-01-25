package com.davesspot.drinklog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.davesspot.drinklog.DrinkApplication
import com.davesspot.drinklog.data.ConsumptionLog
import com.davesspot.drinklog.data.DrinkDao
import com.davesspot.drinklog.data.DrinkType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DrinkViewModel(private val dao: DrinkDao) : ViewModel() {
    val drinkTypes: StateFlow<List<DrinkType>> = dao.getAllDrinkTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<ConsumptionLog>> = dao.getLogsInRange(0, Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // 18 months in milliseconds: 18 * 30.44 * 24 * 60 * 60 * 1000
            val retentionMs = 18L * 30L * 24L * 60L * 60L * 1000L
            val threshold = System.currentTimeMillis() - retentionMs
            dao.deleteOldLogs(threshold)
        }
    }

    fun addDrinkType(name: String, standardUnits: Double) {
        viewModelScope.launch {
            dao.insertDrinkType(DrinkType(name = name, standardUnits = standardUnits))
        }
    }

    fun logDrink(standardUnits: Double) {
        viewModelScope.launch {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.insertConsumption(ConsumptionLog(standardUnits = standardUnits, timestamp = startOfDay))
        }
    }

    fun logDrinkForDate(standardUnits: Double, timestamp: Long) {
        viewModelScope.launch {
            // Ensure timestamp is at start of local day
            val startOfDay = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.insertConsumption(ConsumptionLog(standardUnits = standardUnits, timestamp = startOfDay))
        }
    }

    fun deleteLog(log: ConsumptionLog) {
        viewModelScope.launch {
            dao.deleteConsumption(log)
        }
    }

    fun deleteDrinkType(drinkType: DrinkType) {
        viewModelScope.launch {
            dao.deleteDrinkType(drinkType)
        }
    }

    fun updateLogTimestamp(log: ConsumptionLog, newTimestamp: Long) {
        viewModelScope.launch {
            // newTimestamp is UTC midnight from DatePicker
            val date = Instant.ofEpochMilli(newTimestamp).atZone(ZoneId.of("UTC")).toLocalDate()
            val startOfLocalDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.updateConsumption(log.copy(timestamp = startOfLocalDay))
        }
    }

    fun updateDrinkOrder(reorderedList: List<DrinkType>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, drinkType ->
                dao.updateDrinkType(drinkType.copy(displayOrder = index))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as DrinkApplication)
                DrinkViewModel(app.database.drinkDao())
            }
        }
    }
}
