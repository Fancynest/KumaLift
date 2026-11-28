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

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS workout_schedule_events (
                    id TEXT NOT NULL PRIMARY KEY,
                    routineId TEXT,
                    scheduledDate TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PLANNED',
                    linkedWorkoutId TEXT,
                    routineTitleSnapshot TEXT NOT NULL,
                    splitTypeSnapshot TEXT,
                    notes TEXT,
                    createdAtEpochMs INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL,
                    FOREIGN KEY (routineId) REFERENCES routines(id) ON DELETE SET NULL,
                    FOREIGN KEY (linkedWorkoutId) REFERENCES workouts(id) ON DELETE SET NULL
                );
                """.trimIndent(),
                0
            )
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_workout_schedule_events_date ON workout_schedule_events(scheduledDate);",
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS mesocycles (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    programId TEXT,
                    totalWeeks INTEGER NOT NULL,
                    currentWeek INTEGER NOT NULL,
                    deloadWeek INTEGER NOT NULL,
                    isDeloadActive INTEGER NOT NULL,
                    startDateEpochMs INTEGER NOT NULL,
                    endDateEpochMs INTEGER,
                    status TEXT NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    createdAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_mesocycles_status ON mesocycles(status);",
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS daily_readiness (
                    id TEXT NOT NULL PRIMARY KEY,
                    dateString TEXT NOT NULL,
                    sleepScore INTEGER NOT NULL,
                    sorenessScore INTEGER NOT NULL,
                    energyScore INTEGER NOT NULL,
                    stressScore INTEGER NOT NULL,
                    overallPercentage INTEGER NOT NULL,
                    recommendation TEXT NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    createdAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
            driver.execute(
                null,
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_readiness_date ON daily_readiness(dateString);",
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS safety_snapshots (
                    id TEXT NOT NULL PRIMARY KEY,
                    createdAtEpochMs INTEGER NOT NULL,
                    payloadJson TEXT NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS ai_chat_threads (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    contextSummary TEXT NOT NULL DEFAULT '',
                    messageCount INTEGER NOT NULL DEFAULT 0,
                    createdAtEpochMs INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS ai_chat_messages (
                    id TEXT NOT NULL PRIMARY KEY,
                    threadId TEXT NOT NULL DEFAULT 'thread_default',
                    role TEXT NOT NULL,
                    messageText TEXT NOT NULL,
                    imagePath TEXT,
                    imagePathsJson TEXT NOT NULL DEFAULT '[]',
                    stepLogsJson TEXT NOT NULL DEFAULT '[]',
                    proposalJson TEXT,
                    nutritionJson TEXT,
                    isError INTEGER NOT NULL DEFAULT 0,
                    createdAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}

        try {
            driver.execute(
                null,
                "ALTER TABLE ai_chat_messages ADD COLUMN threadId TEXT NOT NULL DEFAULT 'thread_default';",
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                "ALTER TABLE ai_chat_messages ADD COLUMN imagePathsJson TEXT NOT NULL DEFAULT '[]';",
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_thread ON ai_chat_messages(threadId, createdAtEpochMs);",
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(null, "ALTER TABLE routines ADD COLUMN sharedByCreator TEXT DEFAULT NULL;", 0)
        } catch (_: Exception) {}
        try {
            driver.execute(null, "ALTER TABLE routines ADD COLUMN sharedSourceTitle TEXT DEFAULT NULL;", 0)
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS progress_photos (
                    id TEXT NOT NULL PRIMARY KEY,
                    imagePath TEXT NOT NULL,
                    capturedDateEpochMs INTEGER NOT NULL,
                    bodyweightKg REAL DEFAULT NULL,
                    waistCm REAL DEFAULT NULL,
                    chestCm REAL DEFAULT NULL,
                    armsCm REAL DEFAULT NULL,
                    note TEXT NOT NULL DEFAULT '',
                    isPrivate INTEGER NOT NULL DEFAULT 1,
                    createdAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS idx_progress_photos_date ON progress_photos(capturedDateEpochMs DESC);",
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS exercise_notes (
                    exerciseId TEXT NOT NULL PRIMARY KEY,
                    pinnedNote TEXT NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL
                );
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}
        try {
            driver.execute(
                null,
                "ALTER TABLE sets ADD COLUMN isPr INTEGER NOT NULL DEFAULT 0;",
                0
            )
        } catch (_: Exception) {}
    }
}

object AndroidDatabaseProvider {
    private val lock = Any()
    @Volatile
    private var cachedRepo: KumaDatabaseRepository? = null

    fun getRepository(context: Context): KumaDatabaseRepository {
        cachedRepo?.let { return it }
        return synchronized(lock) {
            cachedRepo ?: run {
                val appContext = context.applicationContext ?: context
                val driver = DatabaseDriverFactory(appContext).createDriver()
                val repo = KumaDatabaseRepository(KumaDatabase(driver))
                cachedRepo = repo
                repo
            }
        }
    }
}
