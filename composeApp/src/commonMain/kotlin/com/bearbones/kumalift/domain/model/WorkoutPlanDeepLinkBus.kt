package com.bearbones.kumalift.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WorkoutPlanDeepLinkBus {
    private val _pendingPlanInput = MutableStateFlow<String?>(null)
    val pendingPlanInput: StateFlow<String?> = _pendingPlanInput.asStateFlow()

    fun submit(rawInput: String?) {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isNotBlank() && WorkoutPlanShareCodec.decodePlanInput(trimmed) != null) {
            _pendingPlanInput.value = trimmed
        }
    }

    fun consume(): String? {
        val current = _pendingPlanInput.value
        _pendingPlanInput.value = null
        return current
    }
}
