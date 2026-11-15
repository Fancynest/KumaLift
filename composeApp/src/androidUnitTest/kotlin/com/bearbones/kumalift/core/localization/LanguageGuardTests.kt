package com.bearbones.kumalift.core.localization

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LanguageGuardTests {

    private fun getFeaturesDirectory(): File {
        val candidates = listOf(
            File("composeApp/src/commonMain/kotlin/com/bearbones/kumalift/features"),
            File("src/commonMain/kotlin/com/bearbones/kumalift/features"),
            File("../composeApp/src/commonMain/kotlin/com/bearbones/kumalift/features")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not find features directory in candidates: ${candidates.map { it.absolutePath }}")
    }

    @Test
    fun testZeroOccurrencesOfIsEnglishInFeatures() {
        val featuresDir = getFeaturesDirectory()
        val ktFiles = featuresDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(ktFiles.isNotEmpty(), "Features directory should contain Kotlin files")

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
            "Found ${violations.size} forbidden occurrences of 'isEnglish' in features code:\n" +
                violations.joinToString("\n")
        )
    }

    @Test
    fun testZeroOccurrencesOfDisplayNameInComposeApp() {
        val commonMainDir = listOf(
            File("composeApp/src/commonMain/kotlin/com/bearbones/kumalift"),
            File("src/commonMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/commonMain/kotlin/com/bearbones/kumalift")
        ).firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not find commonMain directory")

        val ktFiles = commonMainDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(ktFiles.isNotEmpty())

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
            "Found forbidden occurrences of '.displayName' in commonMain:\n" +
                violations.joinToString("\n")
        )
    }

    @Test
    fun testNoHardcodedStringsInTextCallsInFeatures() {
        val featuresDir = getFeaturesDirectory()
        val ktFiles = featuresDir.walkTopDown().filter { it.extension == "kt" }.toList()

        val violations = mutableListOf<String>()
        // Matches Text("...", Text(text = "..."
        val textLiteralRegex = Regex("""Text\(\s*(?:text\s*=\s*)?"([A-Za-z][^"]*)"""")

        // Allowed literal words like app brand name, version, single character markers or formatting tags
        val allowedLiterals = setOf(
            "KumaLift",
            "v1.0.0",
            "W",
            "D",
            "F",
            "RPE",
            "Bio"
        )

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        val matches = textLiteralRegex.findAll(line)
                        for (match in matches) {
                            val literal = match.groupValues[1]
                            if (literal !in allowedLiterals && !allowedLiterals.any { literal.startsWith(it) }) {
                                violations.add("${file.name}:${index + 1}: $line")
                            }
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found ${violations.size} hardcoded Text(\"...\") literals in features:\n" +
                violations.joinToString("\n")
        )
    }
}
