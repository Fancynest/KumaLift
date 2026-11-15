package com.bearbones.kumalift.core.timer

import com.bearbones.kumalift.core.util.currentEpochMillis
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

object RestTimerCoordinator {
    var activeRestTimer: RestTimer? = null

    fun addSeconds(seconds: Int) {
        activeRestTimer?.addSeconds(seconds)
    }

    fun skip() {
        activeRestTimer?.stop()
    }
}

expect fun playTimerCompletionFeedback()
expect fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?)
expect fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int)
expect fun notifyRestTimerStopped()

class RestTimer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(RestTimerState(isRunning = false))
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    var targetEndEpochMs: Long = 0L
        private set

    fun start(seconds: Int = 90, exerciseName: String? = null) {
        countdownJob?.cancel()
        val now = currentEpochMillis()
        targetEndEpochMs = now + seconds * 1000L
        _state.value = RestTimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = true,
            isFinished = false
        )

        RestTimerCoordinator.activeRestTimer = this
        notifyRestTimerStarted(targetEndEpochMs, seconds, exerciseName)

        countdownJob = scope.launch(Dispatchers.Default) {
            while (_state.value.isRunning) {
                delay(250)
                val currentNow = currentEpochMillis()
                val remaining = ((targetEndEpochMs - currentNow + 999L) / 1000L).coerceAtLeast(0L).toInt()
                if (remaining <= 0) {
                    _state.value = _state.value.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    notifyRestTimerStopped()
                    if (RestTimerCoordinator.activeRestTimer === this@RestTimer) {
                        RestTimerCoordinator.activeRestTimer = null
                    }
                    playTimerCompletionFeedback()
                    break
                } else {
                    _state.value = _state.value.copy(remainingSeconds = remaining)
                }
            }
        }
    }

    fun addSeconds(seconds: Int) {
        val current = _state.value
        val newRemaining = (current.remainingSeconds + seconds).coerceAtLeast(0)
        val newTotal = (current.totalSeconds + seconds).coerceAtLeast(newRemaining)
        targetEndEpochMs += seconds * 1000L
        _state.value = current.copy(
            totalSeconds = newTotal,
            remainingSeconds = newRemaining,
            isFinished = newRemaining <= 0
        )
        notifyRestTimerUpdated(targetEndEpochMs, newRemaining)
    }

    fun stop() {
        countdownJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _state.value = RestTimerState(isRunning = false, isFinished = false)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }
}
