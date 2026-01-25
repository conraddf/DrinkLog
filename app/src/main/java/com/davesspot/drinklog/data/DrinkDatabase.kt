package com.davesspot.drinklog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DrinkType::class, ConsumptionLog::class], version = 2, exportSchema = false)
abstract class DrinkDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    companion object {
        @Volatile
        private var Instance: DrinkDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE drink_types ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): DrinkDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, DrinkDatabase::class.java, "drink_log_database")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
