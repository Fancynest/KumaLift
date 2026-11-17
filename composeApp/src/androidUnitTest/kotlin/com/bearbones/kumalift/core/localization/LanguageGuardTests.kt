package com.bearbones.kumalift.core.localization

import com.bearbones.kumalift.data.local.ExerciseDataset
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.TrainingProgram
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Hardened Language Guard Tests (E3 requirement).
 *
 * Ensures:
 * 1. Zero occurrences of forbidden legacy flags `isEnglish` or `.displayName`.
 * 2. Zero hardcoded UI string literals in `Text(...)`, named arguments, or Android Toast/Notifications across commonMain and androidMain.
 * 3. Exact parity between IdStrings and EnStrings: every key defined, no blank values, and identical values strictly bounded to an explicit allowlist.
 */
class LanguageGuardTests {

    private fun getSourceDirectories(): List<File> {
        val baseDirs = listOf(
            File("composeApp/src/commonMain/kotlin/com/bearbones/kumalift"),
            File("composeApp/src/androidMain/kotlin/com/bearbones/kumalift"),
            File("src/commonMain/kotlin/com/bearbones/kumalift"),
            File("src/androidMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/commonMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/androidMain/kotlin/com/bearbones/kumalift")
        )
        val valid = baseDirs.filter { it.exists() && it.isDirectory }
        assertTrue(valid.isNotEmpty(), "Could not find source directories in candidate paths")
        return valid.distinctBy { it.canonicalPath }
    }

    private fun isExcludedFile(file: File): Boolean {
        val path = file.absolutePath.replace('\\', '/')
        return path.contains("/core/localization/") ||
                path.contains("/core/localization") ||
                path.contains("/test/") ||
                path.contains("/androidTest/") ||
                path.contains("/commonTest/") ||
                path.endsWith("DefaultExerciseDatabase.kt") ||
                path.endsWith("ExerciseData.kt")
    }

    private fun getLettersOutsideInterpolation(literal: String): String {
        var cleaned = literal
        while (cleaned.contains("\${")) {
            val start = cleaned.indexOf("\${")
            val end = cleaned.indexOf("}", start)
            if (end != -1) {
                cleaned = cleaned.substring(0, start) + cleaned.substring(end + 1)
            } else {
                break
            }
        }
        cleaned = cleaned.replace(Regex("""\$[A-Za-z0-9_]+"""), "")
        return cleaned.filter { it.isLetter() }
    }

    private fun isDisallowedLiteral(literal: String, allowlist: Set<String>): Boolean {
        if (literal in allowlist) return false
        val letters = getLettersOutsideInterpolation(literal)
        if (letters.isEmpty()) return false
        if (letters in allowlist) return false
        return true
    }

    @Test
    fun testZeroOccurrencesOfIsEnglish() {
        val dirs = getSourceDirectories()
        val ktFiles = dirs.flatMap { it.walkTopDown() }.filter { it.extension == "kt" && !isExcludedFile(it) }

        val violations = mutableListOf<String>()
        val isEnglishRegex = Regex("""\bisEnglish\b""")

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (isEnglishRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found ${violations.size} forbidden occurrences of 'isEnglish' in production code:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testZeroOccurrencesOfDisplayName() {
        val dirs = getSourceDirectories()
        val ktFiles = dirs.flatMap { it.walkTopDown() }.filter { it.extension == "kt" && !isExcludedFile(it) }

        val violations = mutableListOf<String>()
        val displayNameRegex = Regex("""\.displayName\b""")

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (displayNameRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertEquals(
            0,
            violations.size,
            "Found forbidden occurrences of '.displayName' in production code:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testNoHardcodedStringsInUiAndNotifications() {
        val dirs = getSourceDirectories()
        val ktFiles = dirs.flatMap { it.walkTopDown() }.filter { it.extension == "kt" && !isExcludedFile(it) }

        val violations = mutableListOf<String>()

        // 1. Text("...")
        val textLiteralRegex = Regex("""Text\(\s*(?:text\s*=\s*)?"([^"]*[A-Za-z][^"]*)"""")

        // 2. Named arguments in UI calls
        val namedArgRegex = Regex("""(?:text|label|title|placeholder|description|subtitle|message|hint|supportingText|contentDescription)\s*=\s*"([^"]*[A-Za-z][^"]*)"""")

        // 3. Android Notifications & Toasts
        val notifToastRegex = Regex("""(?:Toast\.makeText|setContentTitle|setContentText)\([^"]*"([^"]*[A-Za-z][^"]*)"|(?:contentDescription)\s*=\s*"([^"]*[A-Za-z][^"]*)"""")

        // Exact allowlist of strictly technical / brand non-translatable strings
        val allowlist = setOf(
            "KumaLift",                     // App brand name
            "v1.0.0",                       // Semantic version tag
            "W",                            // Set-type badge glyph (Warm-up)
            "D",                            // Set-type badge glyph (Drop Set)
            "F",                            // Set-type badge glyph (Failure Set)
            "RPE",                          // Universal exercise telemetry acronym
            "SET",                          // Universal athletic column header
            "P",                            // Progress chart period indicator prefix
            "DEL",                          // Numpad delete action key symbol
            "kumalift_rest_timer_ongoing",  // Android notification channel ID
            "kumalift_rest_timer_alert",    // Android notification channel ID
            "kuma_rest_timer_ongoing",      // Android notification channel ID
            "kuma_rest_timer_alert",        // Android notification channel ID
            "workout_reminders",            // Android notification channel ID (legacy)
            "kumalift_backup_alerts",       // Android notification channel ID (legacy)
            "rest_timer_ongoing_v2",        // Android notification channel ID
            "rest_timer_alert_kuma_v2",     // Android notification channel ID
            "rest_timer_alert_vibrate_v2",  // Android notification channel ID
            "rest_timer_alert_silent_v2",   // Android notification channel ID
            "workout_reminder_kuma_v2",     // Android notification channel ID
            "workout_reminder_vibrate_v2",  // Android notification channel ID
            "workout_reminder_silent_v2",   // Android notification channel ID
            "backup_error_v2"               // Android notification channel ID
        )

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        // Check Text(...) calls
                        for (match in textLiteralRegex.findAll(line)) {
                            val literal = match.groupValues[1]
                            if (isDisallowedLiteral(literal, allowlist)) {
                                violations.add("${file.name}:${index + 1} [Text] \"$literal\" in: $trimmed")
                            }
                        }

                        // Check Named arguments
                        for (match in namedArgRegex.findAll(line)) {
                            val literal = match.groupValues[1]
                            if (isDisallowedLiteral(literal, allowlist)) {
                                violations.add("${file.name}:${index + 1} [NamedArg] \"$literal\" in: $trimmed")
                            }
                        }

                        // Check Notifications & Toasts
                        for (match in notifToastRegex.findAll(line)) {
                            val literal = match.groupValues[1].ifEmpty { match.groupValues[2] }
                            if (isDisallowedLiteral(literal, allowlist)) {
                                violations.add("${file.name}:${index + 1} [Notif/Toast] \"$literal\" in: $trimmed")
                            }
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found ${violations.size} hardcoded UI/Notification string literals in production code:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testIdAndEnStringsParityAndNoBlanks() {
        val interfaceClass = AppStrings::class.java
        val zeroArgMethods = interfaceClass.methods
            .filter { it.declaringClass == interfaceClass }
            .filter { it.parameterCount == 0 }

        val idInstance = IdStrings
        val enInstance = EnStrings

        // Bounded allowlist of properties that are identical across languages (brand, ISO units, universal metrics)
        val allowlistedIdenticalKeys = setOf(
            "appName",                      // Brand name "KumaLift"
            "unitKg",                       // ISO metric unit "kg"
            "unitLb",                       // Imperial unit "lb"
            "sizeUnitCm",                   // ISO metric unit "cm"
            "sizeUnitIn",                   // Imperial unit "in"
            "setColumnHeader",              // Universal gym column "SET"
            "rpeColumnHeader",              // Universal exercise science acronym "RPE"
            "metricVolumeLabel",            // Universal athletic volume label "Volume"
            "profileBioFieldPlaceholder",   // Profile biography field "Bio"
            "profileBioLabel",              // Profile biography label "Bio"
            "exerciseMediaAttributionTitle",// Legal copyright attribution notice
            "setTypeDrop",                  // Universal gym terminology "Drop Set"
            "supersetLabel",                // Universal gym terminology "Superset"
            "setTypeWarmupBadge",           // Badge glyph "W"
            "setTypeDropBadge",             // Badge glyph "D"
            "setTypeFailureBadge",          // Badge glyph "F"
            "setTypeDropMenuLabel",         // Drop set menu item "D · Drop Set"
            "targetLabel",                  // Universal word "Target"
            "prBadge",                      // Universal athletic badge "PR"
            "languageOptionIndonesian",     // Native autonym "Bahasa Indonesia"
            "languageOptionEnglish"         // Native autonym "English"
        )

        val blankViolations = mutableListOf<String>()
        val identicalViolations = mutableListOf<String>()

        for (method in zeroArgMethods) {
            val name = method.name
            val propName = name.removePrefix("get").replaceFirstChar { it.lowercase() }

            val idVal = method.invoke(idInstance)
            val enVal = method.invoke(enInstance)

            if (idVal is String && idVal.isBlank()) {
                blankViolations.add("IdStrings.$propName is blank")
            }
            if (enVal is String && enVal.isBlank()) {
                blankViolations.add("EnStrings.$propName is blank")
            }

            if (idVal is String && enVal is String && idVal == enVal) {
                if (propName !in allowlistedIdenticalKeys && name !in allowlistedIdenticalKeys) {
                    identicalViolations.add("Property '$propName' is identical across languages: \"$idVal\"")
                }
            }
        }

        assertTrue(
            blankViolations.isEmpty(),
            "Found blank string properties in AppStrings implementations:\n" + blankViolations.joinToString("\n")
        )

        assertTrue(
            identicalViolations.isEmpty(),
            "Found identical string properties between IdStrings and EnStrings without explicit allowlist:\n" +
                    identicalViolations.joinToString("\n")
        )
    }

    @Test
    fun testIdAndEnParameterizedFunctionsParityAndNoBlanks() {
        val interfaceClass = AppStrings::class.java
        val multiArgMethods = interfaceClass.methods
            .filter { it.declaringClass == interfaceClass }
            .filter { it.parameterCount > 0 }

        val idInstance = IdStrings
        val enInstance = EnStrings

        val allowlistedIdenticalMethods = setOf(
            "weightColumnHeader",         // "KG" / "LB"
            "setTypeNormalBadge",         // "1", "2", "3"
            "setTypeNormalMenuLabel",     // "Normal (1)"
            "supersetBadge",              // "SUPERSET SS"
            "volumeStatLabel",            // "Volume (kg)"
            "recoveryStatusFormat"        // "$status ($pct%)"
        )

        val blankViolations = mutableListOf<String>()
        val identicalViolations = mutableListOf<String>()

        for (method in multiArgMethods) {
            val name = method.name
            val args = method.parameterTypes.map { type ->
                when (type) {
                    Int::class.java, java.lang.Integer.TYPE -> 1
                    Long::class.java, java.lang.Long.TYPE -> 1L
                    Float::class.java, java.lang.Float.TYPE -> 1.0f
                    Double::class.java, java.lang.Double.TYPE -> 1.0
                    Boolean::class.java, java.lang.Boolean.TYPE -> true
                    String::class.java -> {
                        // Use a valid code to trigger branches in when-expressions
                        when (name) {
                            "subMuscleLabel" -> "abs"
                            "equipmentDetailLabel" -> "barbell"
                            "secondaryMuscleDetailLabel" -> "glutes"
                            "howToLogDialogBody" -> "barbell"
                            "howToLogEquipmentTitle" -> "barbell"
                            "howToLogDialogTitle" -> "barbell"
                            else -> "Test"
                        }
                    }
                    else -> "Test"
                }
            }.toTypedArray()

            val idVal = method.invoke(idInstance, *args)
            val enVal = method.invoke(enInstance, *args)

            if (idVal is String && idVal.isBlank()) {
                blankViolations.add("IdStrings.$name(...) is blank")
            }
            if (enVal is String && enVal.isBlank()) {
                blankViolations.add("EnStrings.$name(...) is blank")
            }

            if (idVal is String && enVal is String && idVal == enVal) {
                if (name !in allowlistedIdenticalMethods) {
                    identicalViolations.add("Method '$name' returns identical output across languages: \"$idVal\"")
                }
            }
        }

        assertTrue(
            blankViolations.isEmpty(),
            "Found blank return values in parameterized AppStrings functions:\n" + blankViolations.joinToString("\n")
        )

        assertTrue(
            identicalViolations.isEmpty(),
            "Found identical parameterized function returns without allowlist:\n" +
                    identicalViolations.joinToString("\n")
        )
    }

    @Test
    fun testNoHardcodedDisplayStringsInDomainAndData() {
        val dirs = getSourceDirectories()
        val ktFiles = dirs.flatMap { it.walkTopDown() }
            .filter { it.extension == "kt" }
            .filter { file ->
                val p = file.absolutePath.replace('\\', '/')
                (p.contains("/domain/") || p.contains("/data/")) &&
                        !isExcludedFile(file) &&
                        !p.contains("DatabaseDriverFactory") &&
                        !p.contains("ExerciseDataset") &&
                        !p.contains("DefaultExerciseDatabase")
            }

        val violations = mutableListOf<String>()
        val forbiddenDisplayAssignmentRegex = Regex("""\b(?:displayName|uiTitle|displayMessage)\s*=\s*"([^"]+)"""")

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (forbiddenDisplayAssignmentRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $trimmed")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found forbidden display field assignments in domain/data layer:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testNoRawExceptionMessageLeaksToUi() {
        val dirs = getSourceDirectories()
        val ktFiles = dirs.flatMap { it.walkTopDown() }
            .filter { it.extension == "kt" && !isExcludedFile(it) }

        val violations = mutableListOf<String>()
        val exceptionStateLeakRegex = Regex("""\b(?:statusMessage|errorMessage|userMessage|errorText)\s*=\s*(?:e|ex|err|exception|throwable|t)\.(?:message|localizedMessage)\b""")
        val exceptionUiCallLeakRegex = Regex("""\b(?:Text|Toast\.makeText|showSnackbar)\([^)]*\b(?:e|ex|err|exception|throwable|t)\.(?:message|localizedMessage)\b""")

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (exceptionStateLeakRegex.containsMatchIn(line) || exceptionUiCallLeakRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $trimmed")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found raw exception message leaked to user-visible state or UI component:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testRoutineAndExerciseEntitiesUsePresetKeysAndCodes() {
        // 1. Program seed must have a language-neutral presetKey
        val defaultProgram = TrainingProgram.DEFAULT_HYPERTROPHY
        assertEquals(
            "program_hypertrophy",
            defaultProgram.presetKey,
            "Default training program must use language-neutral presetKey 'program_hypertrophy'"
        )

        // 2. Exercise dataset must use language-neutral uppercase category and equipment codes
        val exercises = ExerciseDataset.allExercises
        assertTrue(exercises.isNotEmpty(), "Exercise dataset must not be empty")

        for (exercise in exercises) {
            assertTrue(
                exercise.primaryMuscle.name.matches(Regex("^[A-Z0-9_]+$")),
                "Exercise primaryMuscle '${exercise.primaryMuscle.name}' in '${exercise.id}' must be an uppercase enum code"
            )
            assertTrue(
                exercise.equipment.name.matches(Regex("^[A-Z0-9_]+$")),
                "Exercise equipment '${exercise.equipment.name}' in '${exercise.id}' must be an uppercase enum code"
            )
            assertTrue(
                exercise.movementType.name.matches(Regex("^[A-Z0-9_]+$")),
                "Exercise movementType '${exercise.movementType.name}' in '${exercise.id}' must be an uppercase enum code"
            )
            for (sm in exercise.secondaryMuscles) {
                assertTrue(
                    sm.name.matches(Regex("^[A-Z0-9_]+$")),
                    "Secondary muscle code '${sm.name}' in '${exercise.id}' must be an uppercase enum code, not translated text"
                )
            }
        }
    }

    @Test
    fun testNumberFormattingUsedForMetricsInUi() {
        val dirs = getSourceDirectories()
        val featureFiles = dirs.flatMap { it.walkTopDown() }
            .filter { it.extension == "kt" && it.absolutePath.replace('\\', '/').contains("/features/") && !isExcludedFile(it) }

        val violations = mutableListOf<String>()
        val rawNumberFormatRegex = Regex("""(\.toString\(\)\s*\+\s*"(?:kg|lb|%)"|\$\{[^}]*(?:weight|volume|oneRm)[^}]*\}\s*(?:kg|lb|%)|String\.format\(|"%[0-9.]*f")""")

        for (file in featureFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (rawNumberFormatRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $trimmed")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found raw number formatting in UI instead of NumberFormatUtils:\n" +
                    violations.joinToString("\n")
        )
    }

    @Test
    fun testTimeUtilsRespectsLanguage() {
        val testEpoch = 1700000000000L
        val idFormatted = com.bearbones.kumalift.core.util.formatEpochToDateString(testEpoch, AppLanguage.ID)
        val enFormatted = com.bearbones.kumalift.core.util.formatEpochToDateString(testEpoch, AppLanguage.EN)

        assertTrue(
            idFormatted != enFormatted,
            "formatEpochToDateString must produce distinct outputs for ID vs EN: '$idFormatted' vs '$enFormatted'"
        )

        // Indonesian uses dd/MM/yyyy format (contains /)
        assertTrue(
            idFormatted.contains("/"),
            "Indonesian date format must use slashes (dd/MM/yyyy): '$idFormatted'"
        )

        // English uses yyyy-MM-dd format (contains -)
        assertTrue(
            enFormatted.contains("-"),
            "English date format must use hyphens (yyyy-MM-dd): '$enFormatted'"
        )
    }
}
