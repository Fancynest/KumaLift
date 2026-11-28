package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.core.util.gzipCompress
import com.bearbones.kumalift.core.util.gzipDecompress
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
enum class SharedPlanType {
    ROUTINE,
    PROGRAM
}

@Serializable
data class SharedSetSpec(
    val setNumber: Int = 1,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restTimerSeconds: Int? = null
)

@Serializable
data class SharedExerciseSpec(
    val exerciseId: String = "",
    val name: String,
    val primaryMuscle: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.BARBELL,
    val movementType: MovementType = MovementType.COMPOUND,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restTimerSeconds: Int = 90,
    val pinnedNote: String = "",
    val setConfigs: List<SharedSetSpec> = emptyList()
)

@Serializable
data class SharedRoutineSpec(
    val sourceRoutineId: String = "",
    val title: String,
    val splitCategory: String = "Custom",
    val muscleFocus: String = "",
    val estimatedDurationMinutes: Int = 45,
    val targetRpeTier: String = "moderate",
    val scheduledDays: List<Int> = emptyList(),
    val instructions: String = "",
    val exercises: List<SharedExerciseSpec> = emptyList()
)

@Serializable
data class SharedWorkoutPlanPackage(
    val schemaVersion: Int = 1,
    val planType: SharedPlanType,
    val title: String,
    val description: String = "",
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val targetDaysPerWeek: Int = 3,
    val activeDays: List<Int> = emptyList(),
    val creatorName: String = "",
    val creatorUsername: String = "",
    val exportedAtEpochMs: Long = 0L,
    val routines: List<SharedRoutineSpec> = emptyList()
) {
    val creatorDisplayLabel: String
        get() = when {
            creatorUsername.isNotBlank() -> "@${creatorUsername.trimStart('@')}"
            creatorName.isNotBlank() -> creatorName.trim()
            else -> ""
        }
}

data class ImportedPlanResult(
    val importedProgram: TrainingProgram? = null,
    val importedRoutines: List<RoutineBlueprint>
)

@OptIn(ExperimentalEncodingApi::class)
object WorkoutPlanShareCodec {
    const val PLAN_CODE_PREFIX = "KUMAPLAN:v1:"
    const val PLAN_CODE_V2_PREFIX = "KUMAPLAN:v2:"
    const val DEEP_LINK_PREFIX = "kumalift://plan?data="

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val compactJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    private val codeTokenV2Regex = Regex("""KUMAPLAN:v2:([A-Za-z0-9\-_+/=]+)""")
    private val codeTokenRegex = Regex("""KUMAPLAN:v1:([A-Za-z0-9\-_+/=]+)""")
    private val deepLinkRegex = Regex("""kumalift://plan\?(?:[^ \n]*&)?data=([A-Za-z0-9\-_%+/=]+)""")
    private val urlSafeBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    private val defaultBase64 = Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun fromRoutine(
        routine: RoutineBlueprint,
        strings: AppStrings,
        profile: ProfileSettings
    ): SharedWorkoutPlanPackage {
        val creatorHandle = profile.username.ifBlank { "" }
        val creatorFull = profile.name.ifBlank { "" }
        val spec = routine.toSharedSpec(strings)
        return SharedWorkoutPlanPackage(
            schemaVersion = 1,
            planType = SharedPlanType.ROUTINE,
            title = spec.title,
            description = spec.muscleFocus,
            goal = profile.goal,
            targetDaysPerWeek = routine.scheduledDays.size.coerceAtLeast(1),
            activeDays = routine.scheduledDays,
            creatorName = creatorFull,
            creatorUsername = creatorHandle,
            exportedAtEpochMs = currentEpochMillis(),
            routines = listOf(spec)
        )
    }

    fun fromProgram(
        program: TrainingProgram,
        routines: List<RoutineBlueprint>,
        strings: AppStrings,
        profile: ProfileSettings
    ): SharedWorkoutPlanPackage {
        val specs = routines.map { it.toSharedSpec(strings) }
        return SharedWorkoutPlanPackage(
            schemaVersion = 1,
            planType = SharedPlanType.PROGRAM,
            title = program.localizedTitle(strings),
            description = program.localizedDescription(strings),
            goal = program.category,
            targetDaysPerWeek = program.targetDaysPerWeek,
            activeDays = program.activeDays,
            creatorName = profile.name,
            creatorUsername = profile.username,
            exportedAtEpochMs = currentEpochMillis(),
            routines = specs
        )
    }

    fun encodePlanCode(pkg: SharedWorkoutPlanPackage): String {
        val payload = json.encodeToString(pkg).encodeToByteArray()
        val b64 = Base64.UrlSafe.encode(payload)
        return "$PLAN_CODE_PREFIX$b64"
    }

    /**
     * Ultra-compact GZIP-compressed token (KUMAPLAN:v2:) omitting default fields & redundant uniform setConfigs
     * so QR modules remain large and fast-scanning while remaining 100% lossless & backwards-compatible.
     */
    fun encodeQrCompactPlanCode(pkg: SharedWorkoutPlanPackage): String {
        val compactPkg = stripRedundantDefaultsForQr(pkg)
        val raw = compactJson.encodeToString(compactPkg).encodeToByteArray()
        val compressed = gzipCompress(raw)
        val noPadUrlSafe = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        return if (compressed != null && compressed.size < raw.size) {
            "$PLAN_CODE_V2_PREFIX${noPadUrlSafe.encode(compressed)}"
        } else {
            "$PLAN_CODE_PREFIX${noPadUrlSafe.encode(raw)}"
        }
    }

    private fun stripRedundantDefaultsForQr(pkg: SharedWorkoutPlanPackage): SharedWorkoutPlanPackage {
        val trimmedRoutines = pkg.routines.map { routine ->
            val trimmedExercises = routine.exercises.map { ex ->
                val isUniformSets = ex.setConfigs.isNotEmpty() && ex.setConfigs.all { sc ->
                    sc.targetReps == ex.targetReps &&
                        sc.targetWeightKg == ex.targetWeightKg &&
                        (sc.restTimerSeconds == null || sc.restTimerSeconds == ex.restTimerSeconds)
                }
                if (isUniformSets) ex.copy(setConfigs = emptyList()) else ex
            }
            routine.copy(sourceRoutineId = "", exercises = trimmedExercises)
        }
        return pkg.copy(exportedAtEpochMs = 0L, routines = trimmedRoutines)
    }

    fun encodeDeepLink(pkg: SharedWorkoutPlanPackage): String {
        val payload = json.encodeToString(pkg).encodeToByteArray()
        val b64 = Base64.UrlSafe.encode(payload)
        return "$DEEP_LINK_PREFIX$b64"
    }

    fun encodePrettyJson(pkg: SharedWorkoutPlanPackage): String {
        val pretty = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
        return pretty.encodeToString(pkg)
    }

    fun buildShareMessage(pkg: SharedWorkoutPlanPackage, strings: AppStrings): String {
        val code = encodePlanCode(pkg)
        val link = encodeDeepLink(pkg)
        val header = pkg.title
        val byLine = pkg.creatorDisplayLabel.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        val exercisesPreview = pkg.routines.joinToString("\n\n") { r ->
            val lines = r.exercises.joinToString("\n") { ex ->
                val load = ex.targetWeightKg?.takeIf { it > 0.0 }?.let { " @ $it ${strings.unitKg}" }.orEmpty()
                "• ${ex.name} (${ex.targetSets}×${ex.targetReps}$load)"
            }
            if (pkg.routines.size > 1) "${r.title}\n$lines" else lines
        }
        return buildString {
            appendLine("$header$byLine — ${strings.appName}")
            if (exercisesPreview.isNotBlank()) {
                appendLine()
                appendLine(exercisesPreview)
            }
            appendLine()
            appendLine(link)
            append(code)
        }
    }

    fun decodePlanInput(rawInput: String): SharedWorkoutPlanPackage? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        // 1. Check KUMAPLAN:v2:<gzip+base64> and KUMAPLAN:v1:<base64> tokens
        codeTokenV2Regex.find(trimmed)?.let { match ->
            decodeBase64Package(match.groupValues[1], compressed = true)?.let { return it }
        }
        codeTokenRegex.find(trimmed)?.let { match ->
            val encoded = match.groupValues[1]
            decodeBase64Package(encoded, compressed = false)?.let { return it }
        }

        // 2. Check kumalift://plan?data=<base64> deep link
        deepLinkRegex.find(trimmed)?.let { match ->
            val rawData = match.groupValues[1]
            val cleaned = rawData
                .replace("%2B", "+", ignoreCase = true)
                .replace("%2F", "/", ignoreCase = true)
                .replace("%3D", "=", ignoreCase = true)
            decodeBase64Package(cleaned, compressed = false)?.let { return it }
        }

        // 3. Raw base64Url if user copied payload without prefix
        if (!trimmed.startsWith("{")) {
            decodeBase64Package(trimmed, compressed = false)?.let { return it }
        }

        // 4. Raw JSON object
        return try {
            val pkg = json.decodeFromString<SharedWorkoutPlanPackage>(trimmed)
            if (pkg.title.isBlank() || pkg.routines.isEmpty()) {
                null
            } else {
                sanitizePackage(pkg).takeIf { it.title.isNotBlank() && it.routines.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64Package(encoded: String, compressed: Boolean = false): SharedWorkoutPlanPackage? {
        return try {
            val rawBytes = try {
                urlSafeBase64.decode(encoded)
            } catch (_: Exception) {
                defaultBase64.decode(encoded)
            }
            val decodedBytes = if (compressed) {
                gzipDecompress(rawBytes) ?: return null
            } else {
                rawBytes
            }
            val pkg = json.decodeFromString<SharedWorkoutPlanPackage>(decodedBytes.decodeToString())
            if (pkg.title.isBlank() || pkg.routines.isEmpty()) {
                null
            } else {
                sanitizePackage(pkg).takeIf { it.title.isNotBlank() && it.routines.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizePackage(pkg: SharedWorkoutPlanPackage): SharedWorkoutPlanPackage {
        val cleanRoutines = pkg.routines
            .filter { it.title.isNotBlank() && it.exercises.isNotEmpty() }
            .take(20)
            .map { r ->
                r.copy(
                    title = r.title.trim().take(80),
                    splitCategory = r.splitCategory.trim().ifBlank { "Custom" }.take(40),
                    muscleFocus = r.muscleFocus.trim().take(120),
                    estimatedDurationMinutes = r.estimatedDurationMinutes.coerceIn(10, 240),
                    scheduledDays = r.scheduledDays.filter { it in 1..7 }.distinct(),
                    instructions = r.instructions.trim().take(500),
                    exercises = r.exercises.filter { it.name.isNotBlank() }.take(40).map { ex ->
                        ex.copy(
                            name = ex.name.trim().take(80),
                            targetSets = ex.targetSets.coerceIn(1, 15),
                            targetReps = ex.targetReps.coerceIn(1, 100),
                            targetWeightKg = ex.targetWeightKg?.takeIf { it.isFinite() && it > 0.0 && it <= 1500.0 },
                            restTimerSeconds = ex.restTimerSeconds.coerceIn(15, 600),
                            pinnedNote = ex.pinnedNote.trim().take(300),
                            setConfigs = ex.setConfigs.take(15).mapIndexed { i, sc ->
                                sc.copy(
                                    setNumber = i + 1,
                                    targetReps = sc.targetReps.coerceIn(1, 100),
                                    targetWeightKg = sc.targetWeightKg?.takeIf { it.isFinite() && it > 0.0 && it <= 1500.0 },
                                    restTimerSeconds = sc.restTimerSeconds?.coerceIn(15, 600)
                                )
                            }
                        )
                    }
                )
            }
            .filter { it.exercises.isNotEmpty() }
        return pkg.copy(
            title = pkg.title.trim().take(80),
            description = pkg.description.trim().take(300),
            targetDaysPerWeek = pkg.targetDaysPerWeek.coerceIn(1, 7),
            activeDays = pkg.activeDays.filter { it in 1..7 }.distinct(),
            routines = cleanRoutines
        )
    }

    private fun RoutineBlueprint.toSharedSpec(strings: AppStrings): SharedRoutineSpec {
        val configs = effectiveConfigs
        val instructionsSummary = configs
            .mapNotNull { c -> c.exercise.pinnedNote.trim().takeIf { it.isNotBlank() }?.let { "${c.exercise.name}: $it" } }
            .joinToString("\n")
        return SharedRoutineSpec(
            sourceRoutineId = id,
            title = localizedTitle(strings),
            splitCategory = localizedSplitCategory(strings),
            muscleFocus = localizedMuscleFocus(strings),
            estimatedDurationMinutes = estimatedDurationMinutes,
            targetRpeTier = targetRpeTier,
            scheduledDays = scheduledDays,
            instructions = instructionsSummary,
            exercises = configs.map { cfg ->
                SharedExerciseSpec(
                    exerciseId = cfg.exercise.id,
                    name = cfg.exercise.name,
                    primaryMuscle = cfg.exercise.primaryMuscle,
                    equipment = cfg.exercise.equipment,
                    movementType = cfg.exercise.movementType,
                    targetSets = cfg.targetSets,
                    targetReps = cfg.targetReps,
                    targetWeightKg = cfg.targetWeightKg,
                    restTimerSeconds = cfg.restTimerSeconds,
                    pinnedNote = cfg.exercise.pinnedNote,
                    setConfigs = cfg.setConfigs.map { s ->
                        SharedSetSpec(
                            setNumber = s.setNumber,
                            targetReps = s.targetReps,
                            targetWeightKg = s.targetWeightKg,
                            restTimerSeconds = s.restTimerSeconds
                        )
                    }
                )
            }
        )
    }
}

object WorkoutPlanShareImporter {
    /**
     * Imports a shared workout package as an independent personal copy with fresh IDs and
     * creator attribution, never modifying any existing creator or recipient routines.
     */
    fun importAsPersonalCopy(
        pkg: SharedWorkoutPlanPackage,
        exerciseRepository: ExerciseRepository,
        routineRepository: RoutineRepository,
        trainingProgramRepository: TrainingProgramRepository,
        settingsRepository: SettingsRepository? = null
    ): ImportedPlanResult {
        val creatorLabel = pkg.creatorDisplayLabel.ifBlank { pkg.title }
        val targetProgram: TrainingProgram? = if (pkg.planType == SharedPlanType.PROGRAM || pkg.routines.size > 1) {
            val createdProg = trainingProgramRepository.createProgram(
                name = pkg.title,
                description = buildString {
                    if (pkg.description.isNotBlank()) append(pkg.description)
                    if (pkg.creatorDisplayLabel.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(pkg.creatorDisplayLabel)
                    }
                },
                category = pkg.goal,
                targetDaysPerWeek = pkg.targetDaysPerWeek,
                activeDays = pkg.activeDays.ifEmpty { listOf(1, 3, 5) }
            )
            trainingProgramRepository.setActiveProgramId(createdProg.id)
            settingsRepository?.updateProfile {
                it.copy(
                    activeProgramId = createdProg.id,
                    trainingDaysPerWeek = createdProg.targetDaysPerWeek,
                    selectedTrainingDays = createdProg.activeDays,
                    goal = createdProg.category
                )
            }
            createdProg
        } else {
            null
        }

        val destinationProgramId = targetProgram?.id ?: trainingProgramRepository.activeProgram.id
        val allExercises = exerciseRepository.exercises.value

        val importedRoutines = pkg.routines.map { routineSpec ->
            val configs = routineSpec.exercises.map { exSpec ->
                val resolvedEx = resolveOrCreateExercise(exSpec, allExercises, exerciseRepository)
                val setConfigs = exSpec.setConfigs.map { sc ->
                    RoutineSetConfig(
                        setNumber = sc.setNumber,
                        targetReps = sc.targetReps,
                        targetWeightKg = sc.targetWeightKg,
                        restTimerSeconds = sc.restTimerSeconds ?: exSpec.restTimerSeconds
                    )
                }
                RoutineExerciseConfig(
                    exercise = resolvedEx,
                    targetSets = exSpec.targetSets,
                    targetReps = exSpec.targetReps,
                    targetWeightKg = exSpec.targetWeightKg,
                    restTimerSeconds = exSpec.restTimerSeconds,
                    setConfigs = setConfigs
                )
            }
            val newId = IdGenerator.routineId(routineSpec.title)
            val blueprint = RoutineBlueprint(
                id = newId,
                programId = destinationProgramId,
                title = routineSpec.title,
                splitCategory = routineSpec.splitCategory,
                muscleFocus = routineSpec.muscleFocus.ifBlank {
                    configs.map { it.exercise.primaryMuscle.name }.distinct().take(3).joinToString(" · ")
                },
                estimatedDurationMinutes = routineSpec.estimatedDurationMinutes,
                targetRpeTier = routineSpec.targetRpeTier,
                exerciseTemplates = configs.map { it.exercise },
                exerciseConfigs = configs,
                plannedSetsPerExercise = configs.firstOrNull()?.targetSets ?: 3,
                scheduledDays = routineSpec.scheduledDays,
                presetKey = null,
                isUserRenamed = true,
                sharedByCreator = creatorLabel.takeIf { it.isNotBlank() },
                sharedSourceTitle = routineSpec.title
            )
            routineRepository.addRoutine(blueprint)
            blueprint
        }

        return ImportedPlanResult(
            importedProgram = targetProgram,
            importedRoutines = importedRoutines
        )
    }

    private fun resolveOrCreateExercise(
        spec: SharedExerciseSpec,
        initialSnapshot: List<Exercise>,
        exerciseRepository: ExerciseRepository
    ): Exercise {
        val currentList = exerciseRepository.exercises.value.ifEmpty { initialSnapshot }
        spec.exerciseId.takeIf { it.isNotBlank() }?.let { id ->
            currentList.firstOrNull { it.id == id }?.let { return it }
        }
        currentList.firstOrNull { it.name.equals(spec.name, ignoreCase = true) }?.let { return it }
        return exerciseRepository.addCustomExercise(
            name = spec.name,
            primaryMuscle = spec.primaryMuscle,
            equipment = spec.equipment,
            movementType = spec.movementType
        ).let { created ->
            if (spec.pinnedNote.isNotBlank()) {
                exerciseRepository.updatePinnedNote(created.id, spec.pinnedNote)
                exerciseRepository.getExerciseById(created.id)
                    ?: created.copy(pinnedNote = spec.pinnedNote)
            } else {
                created
            }
        }
    }
}
