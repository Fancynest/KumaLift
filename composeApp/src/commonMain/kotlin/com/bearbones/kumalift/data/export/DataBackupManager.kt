package com.bearbones.kumalift.data.export

import com.bearbones.kumalift.core.backup.BackupErrorCode
import com.bearbones.kumalift.core.backup.BackupException
import com.bearbones.kumalift.core.crypto.BackupCrypto
import com.bearbones.kumalift.core.crypto.BackupFileType
import com.bearbones.kumalift.core.crypto.BackupInspection
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.ExerciseDataset
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.TrainingProgram
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import com.bearbones.kumalift.domain.model.MesocycleBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class KumaBackupData(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long = 0L,
    val appVersion: String = "2.0.0",
    val settings: UserSettings,
    val workouts: List<Workout> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val customRoutines: List<RoutineBlueprint> = emptyList(),
    val customExercises: List<Exercise> = emptyList(),
    val trainingPrograms: List<TrainingProgram> = emptyList(),
    val scheduledEvents: List<WorkoutScheduleEvent> = emptyList(),
    val mesocycles: List<MesocycleBlock> = emptyList()
)

enum class ImportMode {
    MERGE,
    REPLACE
}

@Serializable
data class ImportResult(
    val success: Boolean,
    val workoutsCount: Int = 0,
    val setsCount: Int = 0,
    val prsCount: Int = 0,
    val routinesCount: Int = 0,
    val exercisesCount: Int = 0,
    val message: String = ""
)

data class StorageUsage(
    val workoutsCount: Int,
    val setsCount: Int,
    val prsCount: Int,
    val estimatedSizeBytes: Long
)

class DataBackupManager(
    private val databaseRepo: KumaDatabaseRepository,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    fun createBackupPayload(settings: UserSettings): KumaBackupData {
        val workouts = databaseRepo.getCompletedWorkouts()
        val prs = databaseRepo.getAllPersonalRecords()
        val customExercises = databaseRepo.getAllCustomExercises()
        val customRoutines = databaseRepo.getAllCustomRoutines { id ->
            customExercises.find { it.id == id } ?: ExerciseDataset.allExercises.find { it.id == id }
        }
        val trainingPrograms = databaseRepo.getAllTrainingPrograms()
        val scheduledEvents = databaseRepo.getAllScheduleEvents()
        val mesocycles = databaseRepo.getAllMesocycles()
        return KumaBackupData(
            schemaVersion = 1,
            exportedAtEpochMs = currentEpochMillis(),
            appVersion = "2.0.0",
            settings = settings,
            workouts = workouts,
            personalRecords = prs,
            customRoutines = customRoutines,
            customExercises = customExercises,
            trainingPrograms = trainingPrograms,
            scheduledEvents = scheduledEvents,
            mesocycles = mesocycles
        )
    }

    fun exportJson(settings: UserSettings): String {
        val payload = createBackupPayload(settings)
        val jsonStr = json.encodeToString(payload)
        KumaLogger.i("DataBackup", "Exported backup JSON (${jsonStr.length} chars, ${payload.workouts.size} workouts)")
        return jsonStr
    }

    fun exportCsvFiles(settings: UserSettings): Map<String, String> {
        val payload = createBackupPayload(settings)
        val files = mutableMapOf<String, String>()

        // 1. workouts.csv
        val workoutSb = StringBuilder()
        workoutSb.append("id,title,splitType,startTimeEpochMs,endTimeEpochMs,durationSeconds,totalVolumeKg,totalSets,totalCompletedSets,avgHeartRateBpm\n")
        payload.workouts.forEach { w ->
            workoutSb.append("\"${w.id}\",\"${w.title}\",\"${w.splitType}\",${w.startTimeEpochMs},${w.endTimeEpochMs ?: 0},${w.durationSeconds},${w.totalVolumeKg},${w.totalSets},${w.totalCompletedSets},${w.avgHeartRateBpm ?: 0}\n")
        }
        files["workouts.csv"] = workoutSb.toString()

        // 2. sets.csv
        val setsSb = StringBuilder()
        setsSb.append("workoutId,exerciseId,exerciseName,setNumber,setType,weightKg,reps,isCompleted,rpe\n")
        payload.workouts.forEach { w ->
            w.exercises.forEach { we ->
                we.sets.forEach { s ->
                    setsSb.append("\"${w.id}\",\"${we.exercise.id}\",\"${we.exercise.name}\",${s.setNumber},\"${s.setType.name}\",${s.weightKg},${s.reps},${s.isCompleted},${s.rpe ?: 0.0}\n")
                }
            }
        }
        files["sets.csv"] = setsSb.toString()

        // 3. personal_records.csv
        val prSb = StringBuilder()
        prSb.append("exerciseId,exerciseName,weightKg,reps,estimated1RmKg,dateAchieved,prTier\n")
        payload.personalRecords.forEach { pr ->
            prSb.append("\"${pr.exerciseId}\",\"${pr.exerciseName}\",${pr.weightKg},${pr.reps},${pr.estimated1RmKg},\"${pr.dateAchieved}\",\"${pr.prTier}\"\n")
        }
        files["personal_records.csv"] = prSb.toString()

        // 4. custom_routines.csv
        val routineSb = StringBuilder()
        routineSb.append("id,title,splitCategory,muscleFocus,estimatedDurationMinutes,targetRpeTier\n")
        payload.customRoutines.forEach { r ->
            routineSb.append("\"${r.id}\",\"${r.title}\",\"${r.splitCategory}\",\"${r.muscleFocus}\",${r.estimatedDurationMinutes},\"${r.targetRpeTier}\"\n")
        }
        files["custom_routines.csv"] = routineSb.toString()

        // 5. custom_exercises.csv
        val exSb = StringBuilder()
        exSb.append("id,name,primaryMuscle,equipment,movementType\n")
        payload.customExercises.forEach { ex ->
            exSb.append("\"${ex.id}\",\"${ex.name}\",\"${ex.primaryMuscle.name}\",\"${ex.equipment.name}\",\"${ex.movementType.name}\"\n")
        }
        files["custom_exercises.csv"] = exSb.toString()

        // 6. schedule_events.csv
        val schedSb = StringBuilder()
        schedSb.append("id,routineId,scheduledDate,status,routineTitleSnapshot,splitTypeSnapshot,notes\n")
        payload.scheduledEvents.forEach { ev ->
            schedSb.append("\"${ev.id}\",\"${ev.routineId ?: ""}\",\"${ev.scheduledDate}\",\"${ev.status.name}\",\"${ev.routineTitleSnapshot}\",\"${ev.splitTypeSnapshot ?: ""}\",\"${ev.notes ?: ""}\"\n")
        }
        files["schedule_events.csv"] = schedSb.toString()

        return files
    }

    fun exportPackage(settings: UserSettings, password: String? = null): ByteArray {
        val jsonBytes = exportJson(settings).encodeToByteArray()
        return if (!password.isNullOrBlank()) {
            BackupCrypto.encryptWithPassword(jsonBytes, password)
        } else {
            BackupCrypto.createUnencryptedContainer(jsonBytes)
        }
    }

    fun unpackBackupPayload(fileBytes: ByteArray, password: String? = null): Result<String> {
        val inspection = BackupCrypto.inspectFile(fileBytes)
        return try {
            when (inspection.type) {
                BackupFileType.PASSWORD_ENCRYPTED -> {
                    if (password.isNullOrBlank()) {
                        Result.failure(BackupException(BackupErrorCode.WRONG_PASSWORD_OR_CORRUPTED, null, "Password required to decrypt backup"))
                    } else {
                        val decrypted = BackupCrypto.decryptWithPassword(fileBytes, password)
                        Result.success(decrypted.decodeToString())
                    }
                }
                BackupFileType.KEYSTORE_ENCRYPTED -> {
                    val decrypted = BackupCrypto.decryptWithKeystore(fileBytes)
                    Result.success(decrypted.decodeToString())
                }
                BackupFileType.UNENCRYPTED_CONTAINER -> {
                    val unpacked = BackupCrypto.unpackUnencryptedContainer(fileBytes)
                    Result.success(unpacked.decodeToString())
                }
                BackupFileType.LEGACY_JSON -> {
                    Result.success(fileBytes.decodeToString())
                }
                BackupFileType.UNKNOWN -> {
                    val str = fileBytes.decodeToString()
                    if (str.trimStart().startsWith("{")) {
                        Result.success(str)
                    } else {
                        Result.failure(BackupException(BackupErrorCode.UNSUPPORTED_FORMAT, null, "Unrecognized backup file format"))
                    }
                }
            }
        } catch (e: BackupException) {
            KumaLogger.e("DataBackupManager", "Unpack failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            KumaLogger.e("DataBackupManager", "Unpack failed: ${e.message}", e)
            Result.failure(BackupException(BackupErrorCode.WRONG_PASSWORD_OR_CORRUPTED, e, e.message))
        }
    }

    fun importBackupPackage(
        fileBytes: ByteArray,
        mode: ImportMode,
        currentSettings: UserSettings,
        password: String? = null,
        strings: AppStrings = resolveAppStrings(currentSettings.appearance.language),
        onSettingsRestored: (UserSettings) -> Unit = {}
    ): ImportResult {
        val unpackResult = unpackBackupPayload(fileBytes, password)
        if (unpackResult.isFailure) {
            val ex = unpackResult.exceptionOrNull()
            val errorCode = (ex as? BackupException)?.errorCode ?: BackupErrorCode.INVALID_FILE
            return ImportResult(
                success = false,
                message = errorCode.localizedMessage(strings)
            )
        }
        val jsonString = unpackResult.getOrThrow()
        return importBackup(jsonString, mode, currentSettings, strings, onSettingsRestored)
    }

    fun importBackup(
        jsonString: String,
        mode: ImportMode,
        currentSettings: UserSettings,
        strings: AppStrings = resolveAppStrings(currentSettings.appearance.language),
        onSettingsRestored: (UserSettings) -> Unit = {}
    ): ImportResult {
        val backup = try {
            json.decodeFromString<KumaBackupData>(jsonString)
        } catch (e: Exception) {
            KumaLogger.e("DataBackup", "Failed to decode backup JSON: ${e.message}", e)
            return ImportResult(
                success = false,
                message = strings.backupInvalidJson(BackupErrorCode.INVALID_FILE.localizedMessage(strings))
            )
        }

        if (backup.schemaVersion > 1) {
            KumaLogger.w("DataBackup", "Backup schema version ${backup.schemaVersion} is newer than current (1)")
            return ImportResult(
                success = false,
                message = strings.backupNewerVersion(backup.schemaVersion)
            )
        }

        return when (mode) {
            ImportMode.REPLACE -> {
                // Step 1: Automatic safety snapshot before destructive replace
                val currentSnapshot = exportJson(currentSettings)
                databaseRepo.saveSafetySnapshot(currentSnapshot)

                // Step 2: Wipe all existing user data
                databaseRepo.wipeAllUserData()

                // Step 3: Insert backup data
                backup.trainingPrograms.forEach { databaseRepo.saveTrainingProgram(it) }
                backup.customExercises.forEach { databaseRepo.saveCustomExercise(it) }
                backup.customRoutines.forEach { databaseRepo.saveCustomRoutine(it) }
                backup.personalRecords.forEach { databaseRepo.savePersonalRecord(it) }
                backup.workouts.forEach { databaseRepo.saveCompletedWorkout(it) }
                backup.scheduledEvents.forEach { databaseRepo.insertScheduleEvent(it) }
                backup.mesocycles.forEach { databaseRepo.insertOrReplaceMesocycle(it) }

                // Step 4: Restore settings
                databaseRepo.saveUserSettings(backup.settings)
                onSettingsRestored(backup.settings)

                val totalSets = backup.workouts.sumOf { it.totalSets }
                ImportResult(
                    success = true,
                    workoutsCount = backup.workouts.size,
                    setsCount = totalSets,
                    prsCount = backup.personalRecords.size,
                    routinesCount = backup.customRoutines.size,
                    exercisesCount = backup.customExercises.size,
                    message = strings.backupReplaceSuccess(backup.customExercises.size, backup.customRoutines.size, backup.personalRecords.size, backup.workouts.size)
                )
            }

            ImportMode.MERGE -> {
                var addedWorkouts = 0
                var addedSets = 0
                val existingWorkouts = databaseRepo.getCompletedWorkouts().map { it.id }.toSet()
                backup.workouts.forEach { w ->
                    if (w.id !in existingWorkouts) {
                        databaseRepo.saveCompletedWorkout(w)
                        addedWorkouts++
                        addedSets += w.totalSets
                    }
                }

                var addedPrs = 0
                val existingPrs = databaseRepo.getAllPersonalRecords().associateBy { it.exerciseId }
                backup.personalRecords.forEach { pr ->
                    val existing = existingPrs[pr.exerciseId]
                    if (existing == null || pr.weightKg > existing.weightKg) {
                        databaseRepo.savePersonalRecord(pr)
                        addedPrs++
                    }
                }

                var addedExercises = 0
                val existingExercises = databaseRepo.getAllCustomExercises().map { it.id }.toSet()
                backup.customExercises.forEach { ex ->
                    if (ex.id !in existingExercises) {
                        databaseRepo.saveCustomExercise(ex)
                        addedExercises++
                    }
                }

                var addedRoutines = 0
                val existingRoutines = databaseRepo.getAllCustomRoutines { null }.map { it.id }.toSet()
                backup.customRoutines.forEach { r ->
                    if (r.id !in existingRoutines) {
                        databaseRepo.saveCustomRoutine(r)
                        addedRoutines++
                    }
                }

                val existingPrograms = databaseRepo.getAllTrainingPrograms().map { it.id }.toSet()
                backup.trainingPrograms.forEach { prog ->
                    if (prog.id !in existingPrograms) {
                        databaseRepo.saveTrainingProgram(prog)
                    }
                }

                val existingEventIds = databaseRepo.getAllScheduleEvents().map { it.id }.toSet()
                backup.scheduledEvents.forEach { ev ->
                    if (ev.id !in existingEventIds) {
                        databaseRepo.insertScheduleEvent(ev)
                    }
                }

                val existingMesocycleIds = databaseRepo.getAllMesocycles().map { it.id }.toSet()
                backup.mesocycles.forEach { meso ->
                    if (meso.id !in existingMesocycleIds) {
                        databaseRepo.insertOrReplaceMesocycle(meso)
                    }
                }

                ImportResult(
                    success = true,
                    workoutsCount = addedWorkouts,
                    setsCount = addedSets,
                    prsCount = addedPrs,
                    routinesCount = addedRoutines,
                    exercisesCount = addedExercises,
                    message = strings.backupMergeSuccess(addedExercises, addedRoutines, addedPrs, addedWorkouts)
                )
            }
        }
    }

    fun restoreSafetySnapshot(
        currentSettings: UserSettings,
        strings: AppStrings = resolveAppStrings(currentSettings.appearance.language),
        onSettingsRestored: (UserSettings) -> Unit = {}
    ): ImportResult {
        val snapshotJson = databaseRepo.getLatestSafetySnapshot()
            ?: return ImportResult(
                false,
                message = strings.noSnapshotAvailable
            )
        return importBackup(snapshotJson, ImportMode.REPLACE, currentSettings, strings, onSettingsRestored)
    }

    fun getStorageUsage(currentSettings: UserSettings): StorageUsage {
        val workouts = databaseRepo.getCompletedWorkouts()
        val setsCount = workouts.sumOf { it.totalSets }
        val prsCount = databaseRepo.getAllPersonalRecords().size
        val jsonPayload = exportJson(currentSettings)
        val estimatedBytes = jsonPayload.encodeToByteArray().size.toLong()

        return StorageUsage(
            workoutsCount = workouts.size,
            setsCount = setsCount,
            prsCount = prsCount,
            estimatedSizeBytes = estimatedBytes
        )
    }
}
