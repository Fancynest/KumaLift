package com.bearbones.kumalift.data.repository

import android.content.Context
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AndroidRoutineStore(
    private val context: Context,
    private val exerciseRepository: ExerciseRepository
) {
    private val file = File(context.filesDir, "custom_routines.json")

    fun loadCustomRoutines(): List<RoutineBlueprint> {
        if (!file.exists()) return emptyList()
        val list = mutableListOf<RoutineBlueprint>()
        try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val splitCategory = obj.optString("splitCategory", "Custom")
                val muscleFocus = obj.optString("muscleFocus", "")
                val duration = obj.optInt("estimatedDurationMinutes", 60)
                val rpeTier = obj.optString("targetRpeTier", "Berat, RPE 8–9")
                val plannedSets = obj.optInt("plannedSetsPerExercise", 3)

                val exercises = mutableListOf<Exercise>()
                val exArray = obj.optJSONArray("exerciseIds")
                if (exArray != null) {
                    for (j in 0 until exArray.length()) {
                        val exId = exArray.getString(j)
                        val found = exerciseRepository.getExerciseById(exId)
                        if (found != null) {
                            exercises.add(found)
                        }
                    }
                }

                list.add(
                    RoutineBlueprint(
                        id = id,
                        title = title,
                        splitCategory = splitCategory,
                        muscleFocus = muscleFocus,
                        estimatedDurationMinutes = duration,
                        targetRpeTier = rpeTier,
                        exerciseTemplates = exercises,
                        plannedSetsPerExercise = plannedSets
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomRoutines(routines: List<RoutineBlueprint>) {
        try {
            val customOnly = routines.filter { it.id.startsWith("custom_") }
            val array = JSONArray()
            for (rt in customOnly) {
                val obj = JSONObject().apply {
                    put("id", rt.id)
                    put("title", rt.title)
                    put("splitCategory", rt.splitCategory)
                    put("muscleFocus", rt.muscleFocus)
                    put("estimatedDurationMinutes", rt.estimatedDurationMinutes)
                    put("targetRpeTier", rt.targetRpeTier)
                    put("plannedSetsPerExercise", rt.plannedSetsPerExercise)
                    val exIds = JSONArray()
                    rt.exerciseTemplates.forEach { exIds.put(it.id) }
                    put("exerciseIds", exIds)
                }
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
