package com.bearbones.kumalift.core.backup

import com.bearbones.kumalift.core.crypto.BackupCrypto
import com.bearbones.kumalift.core.localization.EnStrings
import com.bearbones.kumalift.core.localization.IdStrings
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.export.ImportMode
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.DataBackupSettings
import com.bearbones.kumalift.domain.model.UserSettings
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackupErrorLocalizationTests {

    private val en = EnStrings
    private val id = IdStrings

    private lateinit var database: KumaDatabase
    private lateinit var repository: KumaDatabaseRepository
    private lateinit var backupManager: DataBackupManager

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KumaDatabase.Schema.create(driver)
        database = KumaDatabase(driver)
        repository = KumaDatabaseRepository(database)
        backupManager = DataBackupManager(repository)
    }

    @Test
    fun testAllBackupErrorCodesHaveDistinctTranslations() {
        for (code in BackupErrorCode.entries) {
            val enMsg = code.localizedMessage(en)
            val idMsg = code.localizedMessage(id)

            assertTrue(enMsg.isNotBlank(), "EN message for $code is blank")
            assertTrue(idMsg.isNotBlank(), "ID message for $code is blank")
            assertFalse(enMsg == idMsg, "EN and ID messages for $code should not be identical: $enMsg")
        }
    }

    @Test
    fun testDataBackupSettingsLocalizedError() {
        val settingsWithCode = DataBackupSettings(
            lastBackupResultSuccess = false,
            lastBackupResultError = BackupErrorCode.FOLDER_INACCESSIBLE.name
        )

        assertEquals(
            en.backupErrorFolderInaccessible,
            settingsWithCode.localizedError(en)
        )
        assertEquals(
            id.backupErrorFolderInaccessible,
            settingsWithCode.localizedError(id)
        )
    }

    @Test
    fun testWrongPasswordProducesLocalizedError() {
        val userSettings = UserSettings()
        val encrypted = backupManager.exportPackage(userSettings, "CorrectPassword123")

        val resultEn = backupManager.importBackupPackage(
            fileBytes = encrypted,
            mode = ImportMode.REPLACE,
            currentSettings = userSettings,
            password = "WrongPassword999",
            strings = en
        )
        assertFalse(resultEn.success)
        assertEquals(en.backupErrorWrongPasswordOrCorrupted, resultEn.message)

        val resultId = backupManager.importBackupPackage(
            fileBytes = encrypted,
            mode = ImportMode.REPLACE,
            currentSettings = userSettings,
            password = "WrongPassword999",
            strings = id
        )
        assertFalse(resultId.success)
        assertEquals(id.backupErrorWrongPasswordOrCorrupted, resultId.message)
    }

    @Test
    fun testCorruptedFileProducesLocalizedError() {
        val corruptedBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val userSettings = UserSettings()

        val resultEn = backupManager.importBackupPackage(
            fileBytes = corruptedBytes,
            mode = ImportMode.REPLACE,
            currentSettings = userSettings,
            strings = en
        )
        assertFalse(resultEn.success)
        assertEquals(en.backupErrorUnsupportedFormat, resultEn.message)

        val resultId = backupManager.importBackupPackage(
            fileBytes = corruptedBytes,
            mode = ImportMode.REPLACE,
            currentSettings = userSettings,
            strings = id
        )
        assertFalse(resultId.success)
        assertEquals(id.backupErrorUnsupportedFormat, resultId.message)
    }
}
