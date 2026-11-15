package com.bearbones.kumalift.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.local.DatabaseDriverFactory
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.BackupFrequency
import com.bearbones.kumalift.domain.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AutoBackupRunner {
    private const val TAG = "AutoBackupRunner"

    fun executeBackup(
        context: Context,
        databaseRepo: KumaDatabaseRepository,
        settings: UserSettings
    ): AutoBackupExecutionResult {
        val dataBackup = settings.dataBackup
        val folderUriStr = dataBackup.autoBackupFolderUri
        if (folderUriStr.isNullOrBlank()) {
            val errorMsg = "Folder not configured"
            val updated = settings.copy(
                dataBackup = dataBackup.copy(
                    lastBackupResultSuccess = false,
                    lastBackupResultError = errorMsg
                )
            )
            databaseRepo.saveUserSettings(updated)
            return AutoBackupExecutionResult(false, null, errorMsg)
        }

        val folderUri = Uri.parse(folderUriStr)
        val folderDoc = try {
            val doc = DocumentFile.fromTreeUri(context, folderUri)
            if (doc != null && doc.exists() && doc.canWrite()) doc else null
        } catch (e: Exception) {
            KumaLogger.e(TAG, "Error accessing tree URI", e)
            null
        }

        if (folderDoc == null) {
            val errorMsg = "Folder inaccessible"
            val updated = settings.copy(
                dataBackup = dataBackup.copy(
                    lastBackupResultSuccess = false,
                    lastBackupResultError = errorMsg
                )
            )
            databaseRepo.saveUserSettings(updated)
            return AutoBackupExecutionResult(false, null, errorMsg)
        }

        return try {
            val backupManager = DataBackupManager(databaseRepo)
            val jsonPayload = backupManager.exportJson(settings)

            val sdf = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
            val timestampStr = sdf.format(Date())
            val fileName = "KumaLift-backup-$timestampStr.kumalift"

            val fileDoc = folderDoc.createFile("application/octet-stream", fileName)
                ?: throw IllegalStateException("Cannot create backup file in destination folder")

            context.contentResolver.openOutputStream(fileDoc.uri)?.use { out ->
                out.write(jsonPayload.encodeToByteArray())
            } ?: throw IllegalStateException("Cannot write to destination backup file")

            // Pruning old backups past autoBackupKeepLast
            val keepLast = dataBackup.autoBackupKeepLast.coerceAtLeast(1)
            val existingBackups = folderDoc.listFiles()
                .filter { doc ->
                    val name = doc.name ?: ""
                    name.startsWith("KumaLift-backup-") && name.endsWith(".kumalift")
                }
                .sortedByDescending { it.name ?: "" }

            if (existingBackups.size > keepLast) {
                existingBackups.drop(keepLast).forEach { doc ->
                    try {
                        doc.delete()
                    } catch (e: Exception) {
                        KumaLogger.w(TAG, "Failed to prune older backup: ${doc.name}")
                    }
                }
            }

            val now = System.currentTimeMillis()
            val updated = settings.copy(
                dataBackup = dataBackup.copy(
                    lastBackupEpochMs = now,
                    lastBackupResultSuccess = true,
                    lastBackupResultError = null,
                    lastBackupFileName = fileName
                )
            )
            databaseRepo.saveUserSettings(updated)
            KumaLogger.i(TAG, "Auto-backup succeeded: $fileName")
            AutoBackupExecutionResult(true, fileName, null)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Backup failed"
            KumaLogger.e(TAG, "Auto-backup failed", e)
            val updated = settings.copy(
                dataBackup = dataBackup.copy(
                    lastBackupResultSuccess = false,
                    lastBackupResultError = errorMsg
                )
            )
            databaseRepo.saveUserSettings(updated)
            AutoBackupExecutionResult(false, null, errorMsg)
        }
    }
}

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val driver = DatabaseDriverFactory(applicationContext).createDriver()
            val database = KumaDatabase(driver)
            val databaseRepo = KumaDatabaseRepository(database)
            val settings = databaseRepo.loadUserSettings() ?: return@withContext Result.failure()

            if (!settings.dataBackup.autoBackupEnabled) {
                return@withContext Result.success()
            }

            val execution = AutoBackupRunner.executeBackup(applicationContext, databaseRepo, settings)
            if (execution.success) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            KumaLogger.e("AutoBackupWorker", "Worker execution exception", e)
            Result.failure()
        }
    }
}

object AutoBackupScheduler {
    const val UNIQUE_WORK_NAME = "kumalift_local_auto_backup"

    fun scheduleOrUpdate(context: Context, settings: UserSettings) {
        try {
            val workManager = WorkManager.getInstance(context)
            if (!settings.dataBackup.autoBackupEnabled || settings.dataBackup.autoBackupFolderUri.isNullOrBlank()) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val intervalHours = when (settings.dataBackup.autoBackupFrequency) {
                BackupFrequency.DAILY -> 24L
                BackupFrequency.WEEKLY -> 24L * 7
                BackupFrequency.MONTHLY -> 24L * 30
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            KumaLogger.e("AutoBackupScheduler", "Failed to schedule work", e)
        }
    }

    fun cancel(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            KumaLogger.e("AutoBackupScheduler", "Failed to cancel work", e)
        }
    }
}

actual object AutoBackupService {
    actual fun schedule(settings: UserSettings) {
        val context = AndroidAppProvider.appContext ?: return
        AutoBackupScheduler.scheduleOrUpdate(context, settings)
    }

    actual fun cancel() {
        val context = AndroidAppProvider.appContext ?: return
        AutoBackupScheduler.cancel(context)
    }

    actual suspend fun runBackupNow(
        databaseRepo: KumaDatabaseRepository,
        settings: UserSettings
    ): AutoBackupExecutionResult = withContext(Dispatchers.IO) {
        val context = AndroidAppProvider.appContext
            ?: return@withContext AutoBackupExecutionResult(false, null, "Application context unavailable")
        AutoBackupRunner.executeBackup(context, databaseRepo, settings)
    }
}
