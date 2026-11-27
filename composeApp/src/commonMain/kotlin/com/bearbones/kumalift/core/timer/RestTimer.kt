package com.bearbones.kumalift.core.timer

import com.bearbones.kumalift.core.smartwatch.SmartwatchCompanionManager
import com.bearbones.kumalift.core.util.currentEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.math.abs

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
            val absSec = abs(remainingSeconds)
            val mins = absSec / 60
            val secs = absSec % 60
            val minsStr = if (mins < 10) "0$mins" else "$mins"
            val secsStr = if (secs < 10) "0$secs" else "$secs"
            return if (remainingSeconds < 0) "-$minsStr:$secsStr" else "$minsStr:$secsStr"
        }
}

object RestTimerCoordinator {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sharedTimer: RestTimer by lazy { RestTimer(coordinatorScope) }

    @Volatile
    var activeRestTimer: RestTimer? = null
    var userSettingsProvider: (() -> com.bearbones.kumalift.domain.model.UserSettings)? = null

    fun getOrCreateTimer(): RestTimer {
        return activeRestTimer ?: sharedTimer
    }

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

    fun onAlarmExpired() {
        activeRestTimer?.onAlarmExpired()
    }
}

expect fun playTimerCompletionFeedback()
expect fun playTimerTickFeedback(secondsRemaining: Int)
expect fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?)
expect fun notifyRestTimerPaused(remainingSeconds: Int, exerciseName: String?)
expect fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int)
expect fun notifyRestTimerCompleted(exerciseName: String?, targetEndEpochMs: Long = currentEpochMillis())
expect fun notifyRestTimerStopped()

class RestTimer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(RestTimerState(status = TimerStatus.IDLE))
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    @Volatile
    private var countdownJob: Job? = null
    @Volatile
    private var runGeneration: Long = 0L
    private var lastTickedSecond: Int = -1
    @Volatile
    var targetEndEpochMs: Long = 0L
        private set

    fun start(seconds: Int = 90, exerciseName: String? = null) {
        val safeSeconds = seconds.coerceAtLeast(0)
        startInternal(safeSeconds, exerciseName, safeSeconds)
    }

    private fun startInternal(seconds: Int, exerciseName: String?, totalSeconds: Int) {
        countdownJob?.cancel()
        val gen = ++runGeneration
        lastTickedSecond = -1
        val now = currentEpochMillis()
        val endMs = now + seconds * 1000L
        targetEndEpochMs = endMs
        _state.value = RestTimerState(
            totalSeconds = totalSeconds,
            remainingSeconds = seconds,
            status = TimerStatus.RUNNING,
            exerciseName = exerciseName
        )

        RestTimerCoordinator.activeRestTimer = this
        notifyRestTimerStarted(endMs, totalSeconds, exerciseName)
        launchCountdownLoop(gen)
    }

    private fun launchCountdownLoop(gen: Long) {
        countdownJob = scope.launch(Dispatchers.Default) {
            while (isActive && gen == runGeneration) {
                delay(250)
                if (!isActive || gen != runGeneration) break
                val current = _state.value
                if (current.status != TimerStatus.RUNNING && current.status != TimerStatus.COMPLETED) break
                val diffMs = targetEndEpochMs - currentEpochMillis()
                val remaining = (if (diffMs >= 0L) (diffMs + 999L) / 1000L else diffMs / 1000L).toInt()
                if (current.status == TimerStatus.RUNNING && remaining <= 0) {
                    completeFromRunningIfNeeded(gen)
                } else if (remaining != current.remainingSeconds && gen == runGeneration) {
                    _state.compareAndSet(current, current.copy(remainingSeconds = remaining))
                    if (remaining >= 0) {
                        SmartwatchCompanionManager.sendRestTimerTick(remaining)
                    }
                    if (current.status == TimerStatus.RUNNING && remaining in 1..3 && remaining != lastTickedSecond) {
                        lastTickedSecond = remaining
                        playTimerTickFeedback(remaining)
                    }
                }
            }
        }
    }

    private fun completeFromRunningIfNeeded(expectedGen: Long): Boolean {
        if (expectedGen != runGeneration) return false
        var transitioned = false
        var exerciseName: String? = null
        while (true) {
            if (expectedGen != runGeneration) return false
            val cur = _state.value
            if (cur.status != TimerStatus.RUNNING) break
            val next = cur.copy(remainingSeconds = 0, status = TimerStatus.COMPLETED)
            if (_state.compareAndSet(cur, next)) {
                transitioned = true
                exerciseName = cur.exerciseName
                break
            }
        }
        if (transitioned) {
            playTimerCompletionFeedback()
            SmartwatchCompanionManager.sendHapticPulse()
            notifyRestTimerCompleted(exerciseName, targetEndEpochMs)
        }
        return transitioned
    }

    fun onAlarmExpired() {
        val cur = _state.value
        if (cur.status == TimerStatus.RUNNING && currentEpochMillis() >= targetEndEpochMs) {
            val gen = runGeneration
            if (completeFromRunningIfNeeded(gen) && countdownJob?.isActive != true) {
                launchCountdownLoop(gen)
            }
        }
    }

    fun pause() {
        val cur = _state.value
        if (cur.status == TimerStatus.RUNNING) {
            runGeneration++
            countdownJob?.cancel()
            countdownJob = null
            _state.compareAndSet(cur, cur.copy(status = TimerStatus.PAUSED))
            notifyRestTimerPaused(cur.remainingSeconds, cur.exerciseName)
        }
    }

    fun resume() {
        val cur = _state.value
        if (cur.status == TimerStatus.PAUSED) {
            val remaining = cur.remainingSeconds
            if (remaining > 0) {
                startInternal(remaining, cur.exerciseName, maxOf(cur.totalSeconds, remaining))
            } else {
                val gen = ++runGeneration
                targetEndEpochMs = currentEpochMillis()
                _state.compareAndSet(cur, cur.copy(remainingSeconds = 0, status = TimerStatus.COMPLETED))
                notifyRestTimerCompleted(cur.exerciseName, targetEndEpochMs)
                launchCountdownLoop(gen)
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
        targetEndEpochMs = if (newRemaining <= 0) currentEpochMillis() else targetEndEpochMs + seconds * 1000L
        lastTickedSecond = -1
        if (newRemaining <= 0 && current.status == TimerStatus.RUNNING) {
            val cur = _state.value
            _state.compareAndSet(cur, cur.copy(totalSeconds = newTotal))
            completeFromRunningIfNeeded(runGeneration)
        } else {
            _state.compareAndSet(
                current,
                current.copy(totalSeconds = newTotal, remainingSeconds = newRemaining)
            )
            if (current.status == TimerStatus.RUNNING) {
                notifyRestTimerUpdated(targetEndEpochMs, newRemaining)
            } else if (current.status == TimerStatus.PAUSED) {
                notifyRestTimerPaused(newRemaining, current.exerciseName)
            }
        }
    }

    fun stop() {
        runGeneration++
        countdownJob?.cancel()
        countdownJob = null
        lastTickedSecond = -1
        _state.value = _state.value.copy(status = TimerStatus.IDLE)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }

    fun reset() {
        runGeneration++
        countdownJob?.cancel()
        countdownJob = null
        lastTickedSecond = -1
        _state.value = RestTimerState(status = TimerStatus.IDLE)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }

    fun dismiss() {
        runGeneration++
        countdownJob?.cancel()
        countdownJob = null
        lastTickedSecond = -1
        _state.value = _state.value.copy(status = TimerStatus.DISMISSED)
        notifyRestTimerStopped()
        if (RestTimerCoordinator.activeRestTimer === this) {
            RestTimerCoordinator.activeRestTimer = null
        }
    }
}
