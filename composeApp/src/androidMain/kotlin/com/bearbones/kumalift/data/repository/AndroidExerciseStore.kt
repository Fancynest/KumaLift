package com.bearbones.kumalift.data.repository

import android.content.Context
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AndroidExerciseStore(private val context: Context) {
    private val file = File(context.filesDir, "custom_exercises.json")

    fun loadCustomExercises(): List<Exercise> {
        if (!file.exists()) return emptyList()
        val list = mutableListOf<Exercise>()
        try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val pmName = obj.optString("primaryMuscle", MuscleGroup.CHEST.name)
                val primaryMuscle = try { MuscleGroup.valueOf(pmName) } catch (_: Exception) { MuscleGroup.CHEST }
                val eqName = obj.optString("equipment", Equipment.BARBELL.name)
                val equipment = try { Equipment.valueOf(eqName) } catch (_: Exception) { Equipment.BARBELL }
                val mvName = obj.optString("movementType", MovementType.COMPOUND.name)
                val movementType = try { MovementType.valueOf(mvName) } catch (_: Exception) { MovementType.COMPOUND }

                val secMuscles = mutableListOf<MuscleGroup>()
                val secArray = obj.optJSONArray("secondaryMuscles")
                if (secArray != null) {
                    for (j in 0 until secArray.length()) {
                        try {
                            secMuscles.add(MuscleGroup.valueOf(secArray.getString(j)))
                        } catch (_: Exception) {}
                    }
                }

                list.add(
                    Exercise(
                        id = id,
                        name = name,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscles = secMuscles,
                        secondaryMuscleDetails = secMuscles.joinToString(", ") { it.displayName },
                        equipment = equipment,
                        equipmentDetail = equipment.displayName,
                        movementType = movementType,
                        defaultIncrementKg = 2.5
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomExercises(customExercises: List<Exercise>) {
        try {
            val array = JSONArray()
            for (ex in customExercises) {
                val obj = JSONObject().apply {
                    put("id", ex.id)
                    put("name", ex.name)
                    put("primaryMuscle", ex.primaryMuscle.name)
                    put("equipment", ex.equipment.name)
                    put("movementType", ex.movementType.name)
                    val secArray = JSONArray()
                    ex.secondaryMuscles.forEach { secArray.put(it.name) }
                    put("secondaryMuscles", secArray)
                }
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
