package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.data.repository.DailyReadinessRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DailyReadinessTests {

    @Test
    fun testReadinessCalculationFullIntensity() {
        // Perfect 5/5 across all pillars
        val (percentage, recommendation) = DailyReadinessCheckIn.calculate(5, 5, 5, 5)
        assertEquals(100, percentage)
        assertEquals(ReadinessRecommendation.FULL_INTENSITY, recommendation)

        // 80% boundary
        // sum = 17 -> (13 / 16) * 100 = 81%
        val (pct81, rec81) = DailyReadinessCheckIn.calculate(5, 4, 4, 4)
        assertEquals(81, pct81)
        assertEquals(ReadinessRecommendation.FULL_INTENSITY, rec81)
    }

    @Test
    fun testReadinessCalculationModerateLoad() {
        // 4/5 across all pillars: sum = 16 -> (12 / 16) * 100 = 75%
        val (pct75, rec75) = DailyReadinessCheckIn.calculate(4, 4, 4, 4)
        assertEquals(75, pct75)
        assertEquals(ReadinessRecommendation.MODERATE_LOAD, rec75)

        // 55% boundary: sum = 13 -> (9 / 16) * 100 = 56%
        val (pct56, rec56) = DailyReadinessCheckIn.calculate(4, 3, 3, 3)
        assertEquals(56, pct56)
        assertEquals(ReadinessRecommendation.MODERATE_LOAD, rec56)
    }

    @Test
    fun testReadinessCalculationLightRecovery() {
        // Worst 1/5 across all pillars: sum = 4 -> 0%
        val (pct0, rec0) = DailyReadinessCheckIn.calculate(1, 1, 1, 1)
        assertEquals(0, pct0)
        assertEquals(ReadinessRecommendation.LIGHT_RECOVERY, rec0)

        // Below 55%: sum = 12 -> (8 / 16) * 100 = 50%
        val (pct50, rec50) = DailyReadinessCheckIn.calculate(3, 3, 3, 3)
        assertEquals(50, pct50)
        assertEquals(ReadinessRecommendation.LIGHT_RECOVERY, rec50)
    }

    @Test
    fun testTodayDateStringFormat() {
        val today = DailyReadinessCheckIn.todayDateString()
        assertTrue(today.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun testRepositoryRecordAndReplaceCheckIn() {
        val repo = DailyReadinessRepository()
        val today = DailyReadinessCheckIn.todayDateString()

        // 1. Initial check-in
        val first = repo.recordCheckIn(
            sleep = 5,
            soreness = 5,
            energy = 4,
            stress = 4,
            notes = "Feeling great"
        )
        assertEquals(today, first.dateString)
        assertEquals(87, first.overallPercentage)
        assertEquals(ReadinessRecommendation.FULL_INTENSITY, first.recommendation)
        assertEquals(first, repo.todayCheckIn.value)
        assertEquals(1, repo.allCheckIns.value.size)

        // 2. Overwrite check-in for the same day (re-evaluating later in the day)
        val updated = repo.recordCheckIn(
            sleep = 2,
            soreness = 2,
            energy = 2,
            stress = 2,
            notes = "Afternoon fatigue hit"
        )
        assertEquals(today, updated.dateString)
        assertEquals(25, updated.overallPercentage)
        assertEquals(ReadinessRecommendation.LIGHT_RECOVERY, updated.recommendation)
        assertEquals(updated, repo.todayCheckIn.value)
        assertEquals(1, repo.allCheckIns.value.size) // Should replace, not duplicate
    }

    @Test
    fun testDeleteCheckIn() {
        val repo = DailyReadinessRepository()
        val item = repo.recordCheckIn(5, 5, 5, 5)
        assertEquals(1, repo.allCheckIns.value.size)

        repo.deleteCheckIn(item.id)
        assertTrue(repo.allCheckIns.value.isEmpty())
    }
}
