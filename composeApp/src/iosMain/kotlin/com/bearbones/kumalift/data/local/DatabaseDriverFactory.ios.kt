package com.bearbones.kumalift.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.bearbones.kumalift.database.KumaDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(KumaDatabase.Schema, "kumalift.db")
    }
}
