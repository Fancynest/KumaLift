package com.bearbones.kumalift.data.export

import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.data.local.ExerciseDataset
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
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
    val customExercises: List<Exercise> = emptyList()
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
        return KumaBackupData(
            schemaVersion = 1,
            exportedAtEpochMs = currentEpochMillis(),
            appVersion = "2.0.0",
            settings = settings,
            workouts = workouts,
            personalRecords = prs,
            customRoutines = customRoutines,
            customExercises = customExercises
        )
    }

    fun exportJson(settings: UserSettings): String {
        val payload = createBackupPayload(settings)
        return json.encodeToString(payload)
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

        return files
    }

    fun importBackup(
        jsonString: String,
        mode: ImportMode,
        currentSettings: UserSettings,
        isEnglish: Boolean = resolveIsEnglish(currentSettings.appearance.language),
        onSettingsRestored: (UserSettings) -> Unit = {}
    ): ImportResult {
        val backup = try {
            json.decodeFromString<KumaBackupData>(jsonString)
        } catch (e: Exception) {
            return ImportResult(
                success = false,
                message = if (isEnglish) "Invalid or corrupted JSON format: ${e.message}" else "Format JSON tidak valid atau rusak: ${e.message}"
            )
        }

        if (backup.schemaVersion > 1) {
            return ImportResult(
                success = false,
                message = if (isEnglish)
                    "Backup from a newer app version (v${backup.schemaVersion}) is not supported."
                else
                    "Cadangan dari versi aplikasi yang lebih baru (v${backup.schemaVersion}) tidak didukung."
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
                backup.customExercises.forEach { databaseRepo.saveCustomExercise(it) }
                backup.customRoutines.forEach { databaseRepo.saveCustomRoutine(it) }
                backup.personalRecords.forEach { databaseRepo.savePersonalRecord(it) }
                backup.workouts.forEach { databaseRepo.saveCompletedWorkout(it) }

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
                    message = if (isEnglish)
                        "Successfully restored (Replace): ${backup.workouts.size} workouts, $totalSets sets, and ${backup.personalRecords.size} PRs."
                    else
                        "Berhasil memulihkan (Replace): ${backup.workouts.size} latihan, $totalSets set, dan ${backup.personalRecords.size} PR."
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

                ImportResult(
                    success = true,
                    workoutsCount = addedWorkouts,
                    setsCount = addedSets,
                    prsCount = addedPrs,
                    routinesCount = addedRoutines,
                    exercisesCount = addedExercises,
                    message = if (isEnglish)
                        "Successfully merged (Merge): $addedWorkouts new workouts, $addedSets sets, and $addedPrs personal records."
                    else
                        "Berhasil menggabungkan (Merge): $addedWorkouts latihan baru, $addedSets set, dan $addedPrs rekor PR."
                )
            }
        }
    }

    fun restoreSafetySnapshot(
        currentSettings: UserSettings,
        isEnglish: Boolean = resolveIsEnglish(currentSettings.appearance.language),
        onSettingsRestored: (UserSettings) -> Unit = {}
    ): ImportResult {
        val snapshotJson = databaseRepo.getLatestSafetySnapshot()
            ?: return ImportResult(
                false,
                message = if (isEnglish) "No safety snapshot available." else "Tidak ada snapshot keamanan yang tersedia."
            )
        return importBackup(snapshotJson, ImportMode.REPLACE, currentSettings, isEnglish, onSettingsRestored)
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
