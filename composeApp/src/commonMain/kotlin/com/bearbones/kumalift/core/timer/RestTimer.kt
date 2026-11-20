package com.bearbones.kumalift.core.timer

import com.bearbones.kumalift.core.smartwatch.SmartwatchCompanionManager
import com.bearbones.kumalift.core.util.currentEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    DISMISSED
}

data class RestTimerState(
    val totalSeconds: Int = 90,
    val remainingSeconds: Int = 90,
    val status: TimerStatus = TimerStatus.IDLE,
    val exerciseName: String? = null
) {
    val isRunning: Boolean get() = status == TimerStatus.RUNNING
    val isPaused: Boolean get() = status == TimerStatus.PAUSED
    val isFinished: Boolean get() = status == TimerStatus.COMPLETED
    val isVisible: Boolean get() = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED || status == TimerStatus.COMPLETED

    constructor(
        totalSeconds: Int = 90,
        remainingSeconds: Int = 90,
        isRunning: Boolean = false,
        isFinished: Boolean = false
    ) : this(
        totalSeconds = totalSeconds,
        remainingSeconds = remainingSeconds,
        status = when {
            isFinished -> TimerStatus.COMPLETED
            isRunning -> TimerStatus.RUNNING
            else -> TimerStatus.IDLE
        },
        exerciseName = null
    )

    val progressFraction: Float
        get() = if (status == TimerStatus.COMPLETED) {
            1f
        } else if (totalSeconds > 0) {
            (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
        } else 0f

    val formattedTime: String
        get() {
            if (status == TimerStatus.COMPLETED) return "00:00"
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val minsStr = if (mins < 10) "0$mins" else "$mins"
            val secsStr = if (secs < 10) "0$secs" else "$secs"
            return "$minsStr:$secsStr"
        }
}

object RestTimerCoordinator {
    var activeRestTimer: RestTimer? = null
    var userSettingsProvider: (() -> com.bearbones.kumalift.domain.model.UserSettings)? = null

    fun addSeconds(seconds: Int) {
        activeRestTimer?.addSeconds(seconds)
    }

    fun pause() {
        activeRestTimer?.pause()
    }

    fun resume() {
        activeRestTimer?.resume()
    }

    fun skip() {
        activeRestTimer?.stop()
    }

    fun dismiss() {
        activeRestTimer?.dismiss()
    }
}

expect fun playTimerCompletionFeedback()
expect fun playTimerTickFeedback(secondsRemaining: Int)
expect fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?)
expect fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int)
expect fun notifyRestTimerCompleted(exerciseName: String?)
expect fun notifyRestTimerStopped()

class RestTimer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(RestTimerState(status = TimerStatus.IDLE))
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var lastTickedSecond: Int = -1
    var targetEndEpochMs: Long = 0L
        private set

    fun start(seconds: Int = 90, exerciseName: String? = null) {
        countdownJob?.cancel()
        lastTickedSecond = -1
        val now = currentEpochMillis()
        targetEndEpochMs = now + seconds * 1000L
        _state.value = RestTimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            status = TimerStatus.RUNNING,
            exerciseName = exerciseName
        )

        RestTimerCoordinator.activeRestTimer = this
        notifyRestTimerStarted(targetEndEpochMs, seconds, exerciseName)

        countdownJob = scope.launch(Dispatchers.Default) {
            while (_state.value.status == TimerStatus.RUNNING) {
                delay(250)
                val currentNow = currentEpochMillis()
                val remaining = ((targetEndEpochMs - currentNow + 999L) / 1000L).coerceAtLeast(0L).toInt()
                if (remaining <= 0) {
                    _state.value = _state.value.copy(
                        remainingSeconds = 0,
                        status = TimerStatus.COMPLETED
                    )
                    playTimerCompletionFeedback()
                    SmartwatchCompanionManager.sendHapticPulse()
                    notifyRestTimerCompleted(_state.value.exerciseName)
                    break
                } else {
                    val prev = _state.value.remainingSeconds
                    _state.value = _state.value.copy(remainingSeconds = remaining)
                    if (remaining != prev) {
                        SmartwatchCompanionManager.sendRestTimerTick(remaining)
                    }
                    if (remaining in 1..3 && remaining != lastTickedSecond) {
                        lastTickedSecond = remaining
                        playTimerTickFeedback(remaining)
                    }
                }
            }
        }
    }

    fun pause() {
        if (_state.value.status == TimerStatus.RUNNING) {
            countdownJob?.cancel()
            _state.value = _state.value.copy(status = TimerStatus.PAUSED)
        }
    }

    fun resume() {
        if (_state.value.status == TimerStatus.PAUSED) {
            val remaining = _state.value.remainingSeconds
            if (remaining > 0) {
                start(remaining, _state.value.exerciseName)
            } else {
                _state.value = _state.value.copy(status = TimerStatus.COMPLETED)
            }
        }
    }

    fun addSeconds(seconds: Int) {
        val current = _state.value
        if (current.status == TimerStatus.COMPLETED) {
            if (seconds > 0) {
                start(seconds, current.exerciseName)
            }
            return
        }
        val newRemaining = (current.remainingSeconds + seconds).coerceAtLeast(0)
        val newTotal = (current.totalSeconds + seconds).coerceAtLeast(newRemaining)
        targetEndEpochMs += seconds * 1000L
        lastTickedSecond = -1
        if (newRemaining <= 0 && current.status == TimerStatus.RUNNING) {
            countdownJob?.cancel()
            _state.value = current.copy(
                totalSeconds = newTotal,
                remainingSeconds = 0,
                status = TimerStatus.COMPLETED
            )
            playTimerCompletionFeedback()
            notifyRestTimerCompleted(current.exerciseName)
        } else {
            _state.value = current.copy(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining
            )
            if (current.status == TimerStatus.RUNNING) {
                notifyRestTimerUpdated(targetEndEpochMs, newRemaining)
            }
        }
    }

    fun stop() {
        countdownJob?.cancel()
        lastTickedSecond = -1
        _state.value = _state.value.copy(status = TimerStatus.IDLE)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }

    fun reset() {
        countdownJob?.cancel()
        lastTickedSecond = -1
        _state.value = RestTimerState(status = TimerStatus.IDLE)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }

    fun dismiss() {
        countdownJob?.cancel()
        lastTickedSecond = -1
        _state.value = _state.value.copy(status = TimerStatus.DISMISSED)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }
}
