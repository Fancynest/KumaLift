package com.bearbones.kumalift.core.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestTimerState(
    val totalSeconds: Int = 90,
    val remainingSeconds: Int = 90,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
        } else 0f

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val minsStr = if (mins < 10) "0$mins" else "$mins"
            val secsStr = if (secs < 10) "0$secs" else "$secs"
            return "$minsStr:$secsStr"
        }
}

expect fun playTimerCompletionFeedback()

class RestTimer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(RestTimerState(isRunning = false))
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun start(seconds: Int = 90) {
        countdownJob?.cancel()
        _state.value = RestTimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = true,
            isFinished = false
        )

        countdownJob = scope.launch(Dispatchers.Default) {
            while (_state.value.remainingSeconds > 0 && _state.value.isRunning) {
                delay(1000)
                val currentRemaining = _state.value.remainingSeconds - 1
                if (currentRemaining <= 0) {
                    _state.value = _state.value.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    playTimerCompletionFeedback()
                } else {
                    _state.value = _state.value.copy(remainingSeconds = currentRemaining)
                }
            }
        }
    }

    fun addSeconds(seconds: Int) {
        val current = _state.value
        val newRemaining = (current.remainingSeconds + seconds).coerceAtLeast(0)
        val newTotal = (current.totalSeconds + seconds).coerceAtLeast(newRemaining)
        _state.value = current.copy(
            totalSeconds = newTotal,
            remainingSeconds = newRemaining,
            isFinished = newRemaining <= 0
        )
    }

    fun stop() {
        countdownJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        countdownJob?.cancel()
        _state.value = RestTimerState(isRunning = false, isFinished = false)
    }
}
