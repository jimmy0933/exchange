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
    version = 2,
    exportSchema = true
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
                ).addMigrations(MIGRATION_1_2)
                 .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rate_history ADD COLUMN source TEXT NOT NULL DEFAULT '台北富邦銀行'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rate_history_currencyCode_timestamp` ON `rate_history` (`currencyCode`, `timestamp`)")
            }
        }
    }
}
