package com.bearbones.kumalift.features.library

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryFilterStateTests {

    private fun sampleExercise(
        id: String = "bench_press",
        name: String = "Barbell Bench Press",
        primaryMuscle: MuscleGroup = MuscleGroup.CHEST,
        subMuscle: String = "Mid chest",
        equipment: Equipment = Equipment.BARBELL,
        movementType: MovementType = MovementType.COMPOUND
    ): Exercise = Exercise(
        id = id,
        name = name,
        primaryMuscle = primaryMuscle,
        subMuscle = subMuscle,
        equipment = equipment,
        movementType = movementType
    )

    // =========================================================================
    // Requirement 9.A: Single Source of Truth & Filter Logic
    // =========================================================================

    @Test
    fun testInitialStateAndActiveFlag() {
        val state = LibraryFilterState()
        assertEquals("", state.searchQuery)
        assertNull(state.selectedMuscle)
        assertNull(state.selectedEquipment)
        assertNull(state.selectedMovementType)
        assertFalse(state.isActive)

        state.searchQuery = "bench"
        assertTrue(state.isActive)

        state.searchQuery = ""
        assertFalse(state.isActive)

        state.selectedMuscle = MuscleGroup.CHEST
        assertTrue(state.isActive)

        state.selectedEquipment = Equipment.BARBELL
        assertTrue(state.isActive)

        state.selectedMovementType = MovementType.COMPOUND
        assertTrue(state.isActive)
    }

    @Test
    fun testResetClearsAllFilters() {
        val state = LibraryFilterState(
            initialSearchQuery = "curl",
            initialMuscle = MuscleGroup.ARMS,
            initialEquipment = Equipment.DUMBBELL,
            initialMovementType = MovementType.ISOLATION
        )
        assertTrue(state.isActive)

        state.reset()

        assertEquals("", state.searchQuery)
        assertNull(state.selectedMuscle)
        assertNull(state.selectedEquipment)
        assertNull(state.selectedMovementType)
        assertFalse(state.isActive)
    }

    @Test
    fun testMatchesFiltersCorrectly() {
        val state = LibraryFilterState()
        val bench = sampleExercise(
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        val squat = sampleExercise(
            id = "squat",
            name = "Barbell Back Squat",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        val bicepCurl = sampleExercise(
            id = "curl",
            name = "Dumbbell Bicep Curl",
            primaryMuscle = MuscleGroup.ARMS,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.ISOLATION
        )

        // Empty filter matches all
        assertTrue(state.matches(bench))
        assertTrue(state.matches(squat))
        assertTrue(state.matches(bicepCurl))

        // Muscle filter
        state.selectedMuscle = MuscleGroup.CHEST
        assertTrue(state.matches(bench))
        assertFalse(state.matches(squat))
        assertFalse(state.matches(bicepCurl))

        // Equipment filter
        state.selectedMuscle = null
        state.selectedEquipment = Equipment.DUMBBELL
        assertFalse(state.matches(bench))
        assertFalse(state.matches(squat))
        assertTrue(state.matches(bicepCurl))

        // MovementType filter
        state.selectedEquipment = null
        state.selectedMovementType = MovementType.COMPOUND
        assertTrue(state.matches(bench))
        assertTrue(state.matches(squat))
        assertFalse(state.matches(bicepCurl))

        // Search Query (case-insensitive substring)
        state.selectedMovementType = null
        state.searchQuery = "  squat  "
        assertFalse(state.matches(bench))
        assertTrue(state.matches(squat))
        assertFalse(state.matches(bicepCurl))
    }

    // =========================================================================
    // Requirement 9.D: State Restoration & Enum Compatibility Saver
    // =========================================================================

    @Test
    fun testSaverSerializesEnumsToStringNames() {
        val state = LibraryFilterState(
            initialSearchQuery = "press",
            initialMuscle = MuscleGroup.SHOULDERS,
            initialEquipment = Equipment.DUMBBELL,
            initialMovementType = MovementType.COMPOUND
        )

        val saverScope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(LibraryFilterState.Saver) { saverScope.save(state) } as? List<*>

        assertTrue(saved != null)
        assertEquals(4, saved.size)
        assertEquals("press", saved[0])
        assertEquals("SHOULDERS", saved[1])
        assertEquals("DUMBBELL", saved[2])
        assertEquals("COMPOUND", saved[3])
    }

    @Test
    fun testSaverRestoresValidStateSuccessfully() {
        val serialized = listOf("row", "BACK", "CABLE", "COMPOUND")
        val restored = LibraryFilterState.Saver.restore(serialized)

        assertTrue(restored != null)
        assertEquals("row", restored.searchQuery)
        assertEquals(MuscleGroup.BACK, restored.selectedMuscle)
        assertEquals(Equipment.CABLE, restored.selectedEquipment)
        assertEquals(MovementType.COMPOUND, restored.selectedMovementType)
        assertTrue(restored.isActive)
    }

    @Test
    fun testSaverHandlesNullEnumsGracefully() {
        val serialized = listOf("squat", null, null, null)
        val restored = LibraryFilterState.Saver.restore(serialized)

        assertTrue(restored != null)
        assertEquals("squat", restored.searchQuery)
        assertNull(restored.selectedMuscle)
        assertNull(restored.selectedEquipment)
        assertNull(restored.selectedMovementType)
        assertTrue(restored.isActive)
    }

    @Test
    fun testSaverHandlesDeprecatedOrInvalidEnumGracefullyWithoutCrash() {
        // Requirement 9.D: Handle enum values no longer available due to version changes
        val serializedWithDeprecatedValues = listOf(
            "bench",
            "NON_EXISTENT_MUSCLE_GROUP",
            "DEPRECATED_EQUIPMENT_NAME",
            "UNKNOWN_MOVEMENT_TYPE"
        )

        // Must NOT throw IllegalArgumentException; must safely fall back to null
        val restored = LibraryFilterState.Saver.restore(serializedWithDeprecatedValues)

        assertTrue(restored != null)
        assertEquals("bench", restored.searchQuery)
        assertNull(restored.selectedMuscle)
        assertNull(restored.selectedEquipment)
        assertNull(restored.selectedMovementType)
    }

    @Test
    fun testSaverHandlesEmptyOrCorruptedList() {
        val emptyList = emptyList<Any?>()
        val restored = LibraryFilterState.Saver.restore(emptyList)

        assertTrue(restored != null)
        assertEquals("", restored.searchQuery)
        assertNull(restored.selectedMuscle)
        assertNull(restored.selectedEquipment)
        assertNull(restored.selectedMovementType)
        assertFalse(restored.isActive)
    }
}
