package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlateCount(
    val weightKg: Double,
    val countPerSide: Int,
    val hexColor: Long,
    val heightFraction: Float // 0.3f to 1.0f for proportional visual drawing
)

@Serializable
data class PlateCalculationResult(
    val targetWeightKg: Double,
    val barWeightKg: Double,
    val weightToLoadKg: Double,
    val weightPerSideKg: Double,
    val platesPerSide: List<PlateCount>,
    val totalCalculatedWeightKg: Double,
    val remainderKg: Double
) {
    val isExactMatch: Boolean
        get() = remainderKg < 0.01
}

object PlateCalculator {

    // Standard plate specs: weight, hex color, visual height fraction
    val STANDARD_METRIC_PLATES = listOf(
        Triple(25.0, 0xFFE53935, 1.00f), // Red
        Triple(20.0, 0xFF1E88E5, 1.00f), // Blue
        Triple(15.0, 0xFFFDD835, 0.90f), // Yellow
        Triple(10.0, 0xFF43A047, 0.78f), // Green
        Triple(5.0,  0xFFFAFAFA, 0.60f), // White
        Triple(2.5,  0xFF424242, 0.48f), // Charcoal/Black
        Triple(1.25, 0xFF9E9E9E, 0.38f), // Silver
        Triple(0.5,  0xFFBDBDBD, 0.30f)  // Micro
    )

    val STANDARD_IMPERIAL_PLATES = listOf(
        Triple(45.0, 0xFF1E88E5, 1.00f), // Blue
        Triple(35.0, 0xFFFDD835, 0.90f), // Yellow
        Triple(25.0, 0xFF43A047, 0.78f), // Green
        Triple(10.0, 0xFFFAFAFA, 0.60f), // White
        Triple(5.0,  0xFF424242, 0.48f), // Charcoal/Black
        Triple(2.5,  0xFF9E9E9E, 0.38f), // Silver
        Triple(1.25, 0xFFBDBDBD, 0.30f)  // Micro
    )

    val PRESET_BARS = listOf(
        BarEquipment("olympic", "Olympic Bar (20 kg)", 20.0),
        BarEquipment("womens", "Women's Bar (15 kg)", 15.0),
        BarEquipment("technique", "EZ / Technique Bar (10 kg)", 10.0),
        BarEquipment("custom", "Custom / Machine", 0.0)
    )

    /**
     * Calculates the required plates per side to reach [targetWeightKg] using [barWeightKg].
     * Greedy algorithm selects largest available plates first.
     */
    fun calculate(
        targetWeightKg: Double,
        barWeightKg: Double = 20.0,
        availablePlatesKg: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
        useImperialPalette: Boolean = false
    ): PlateCalculationResult {
        val safeTarget = (if (targetWeightKg.isFinite()) targetWeightKg else 0.0).coerceIn(0.0, 2000.0)
        val safeBar = (if (barWeightKg.isFinite()) barWeightKg else 0.0).coerceIn(0.0, 500.0)

        val weightToLoad = (safeTarget - safeBar).coerceAtLeast(0.0)
        var remainingPerSide = weightToLoad / 2.0

        val sortedPlates = availablePlatesKg.filter { it.isFinite() && it > 0.0 }.distinct().sortedDescending()
        val palette = if (useImperialPalette) STANDARD_IMPERIAL_PLATES else STANDARD_METRIC_PLATES
        val resultPlates = mutableListOf<PlateCount>()

        for (plateWeight in sortedPlates) {
            val count = ((remainingPerSide + 1e-4) / plateWeight).toInt()
            if (count > 0) {
                val spec = palette.firstOrNull { it.first == plateWeight }
                val color = spec?.second ?: 0xFF90A4AE
                val heightFrac = spec?.third ?: 0.5f

                resultPlates.add(
                    PlateCount(
                        weightKg = plateWeight,
                        countPerSide = count,
                        hexColor = color,
                        heightFraction = heightFrac
                    )
                )
                remainingPerSide = (remainingPerSide - count * plateWeight).coerceAtLeast(0.0)
            }
        }

        val totalLoadedPerSide = resultPlates.sumOf { it.weightKg * it.countPerSide }
        val totalCalculated = safeBar + (totalLoadedPerSide * 2.0)
        val remainder = (safeTarget - totalCalculated).coerceAtLeast(0.0)

        return PlateCalculationResult(
            targetWeightKg = safeTarget,
            barWeightKg = safeBar,
            weightToLoadKg = weightToLoad,
            weightPerSideKg = totalLoadedPerSide,
            platesPerSide = resultPlates,
            totalCalculatedWeightKg = totalCalculated,
            remainderKg = remainder
        )
    }
}
