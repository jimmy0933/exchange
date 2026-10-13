package com.fubon.fxmonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LatestRateEntity::class,
        RateHistoryEntity::class,
        ExchangeAlertEntity::class,
        AlertHistoryEntity::class,
        ErrorLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao
    abstract fun alertDao(): AlertDao
    abstract fun errorLogDao(): ErrorLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fubon_fx_monitor.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
