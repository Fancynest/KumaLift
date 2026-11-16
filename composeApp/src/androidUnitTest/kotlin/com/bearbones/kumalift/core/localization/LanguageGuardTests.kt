package com.bearbones.kumalift.core.localization

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
            "kuma_rest_timer_alert"         // Android notification channel ID
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
            "prBadge"                       // Universal athletic badge "PR"
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
}
