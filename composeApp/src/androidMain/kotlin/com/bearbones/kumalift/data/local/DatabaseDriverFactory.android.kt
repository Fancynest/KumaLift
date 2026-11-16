package com.bearbones.kumalift.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bearbones.kumalift.database.KumaDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(KumaDatabase.Schema, context, "kumalift.db")
        migrateIfNeeded(driver)
        return driver
    }

    private fun migrateIfNeeded(driver: SqlDriver) {
        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS training_programs (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    targetDaysPerWeek INTEGER NOT NULL DEFAULT 3,
                    activeDays TEXT NOT NULL DEFAULT '1,3,5',
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    createdAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                "ALTER TABLE training_programs ADD COLUMN activeDays TEXT NOT NULL DEFAULT '1,3,5';",
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                "ALTER TABLE routines ADD COLUMN programId TEXT NOT NULL DEFAULT 'program_hypertrophy_default';",
                0
            )
        } catch (_: Exception) {
            // Column already exists
        }
    }
}
