package com.bearbones.kumalift.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.bearbones.kumalift.database.KumaDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(KumaDatabase.Schema, "kumalift.db")
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

        try {
            driver.execute(
                null,
                "ALTER TABLE routines ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT '';",
                0
            )
        } catch (_: Exception) {
            // Column already exists
        }

        try {
            driver.execute(
                null,
                "ALTER TABLE routine_exercises ADD COLUMN restTimerSeconds INTEGER NOT NULL DEFAULT 90;",
                0
            )
        } catch (_: Exception) {
            // Column already exists
        }

        try {
            driver.execute(
                null,
                "ALTER TABLE routine_exercises ADD COLUMN setConfigsJson TEXT NOT NULL DEFAULT '';",
                0
            )
        } catch (_: Exception) {
            // Column already exists
        }

        try {
            driver.execute(null, "ALTER TABLE training_programs ADD COLUMN presetKey TEXT DEFAULT NULL;", 0)
        } catch (_: Exception) {}

        try {
            driver.execute(null, "ALTER TABLE training_programs ADD COLUMN isUserRenamed INTEGER NOT NULL DEFAULT 0;", 0)
        } catch (_: Exception) {}

        try {
            driver.execute(null, "ALTER TABLE routines ADD COLUMN presetKey TEXT DEFAULT NULL;", 0)
        } catch (_: Exception) {}

        try {
            driver.execute(null, "ALTER TABLE routines ADD COLUMN isUserRenamed INTEGER NOT NULL DEFAULT 0;", 0)
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                "UPDATE training_programs SET presetKey = 'program_hypertrophy' WHERE id = 'program_hypertrophy_default' AND (presetKey IS NULL OR presetKey = '');",
                0
            )
            driver.execute(
                null,
                "UPDATE routines SET presetKey = 'starter_push' WHERE id = 'starter_push' AND (presetKey IS NULL OR presetKey = '');",
                0
            )
            driver.execute(
                null,
                "UPDATE routines SET presetKey = 'starter_pull' WHERE id = 'starter_pull' AND (presetKey IS NULL OR presetKey = '');",
                0
            )
            driver.execute(
                null,
                "UPDATE routines SET presetKey = 'starter_strength_a' WHERE id = 'starter_strength_a' AND (presetKey IS NULL OR presetKey = '');",
                0
            )
            driver.execute(
                null,
                "UPDATE routines SET presetKey = 'starter_general_full' WHERE id = 'starter_general_full' AND (presetKey IS NULL OR presetKey = '');",
                0
            )
        } catch (_: Exception) {}
    }
}
