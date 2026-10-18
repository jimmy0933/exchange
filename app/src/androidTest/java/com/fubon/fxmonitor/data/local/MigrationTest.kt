package com.fubon.fxmonitor.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)

        // We can insert some dummy data into version 1 here
        db.execSQL("INSERT INTO rate_history (currencyCode, spotBuy, spotSell, cashBuy, cashSell, officialUpdateTime, timestamp, isOfficialDaily) VALUES ('USD', '30.0', '30.5', '29.9', '30.6', '2023-01-01 10:00:00', 1672538400000, 0)")

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT source FROM rate_history WHERE currencyCode = 'USD'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(0) == "台北富邦銀行")
        cursor.close()
    }
}
