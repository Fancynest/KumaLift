package com.bearbones.kumalift.core.timer

import com.bearbones.kumalift.core.sound.PlatformSoundPlayer

actual fun playTimerCompletionFeedback() {
    PlatformSoundPlayer.playSystemDefaultSound(100)
    PlatformSoundPlayer.playHaptic()
}

actual fun playTimerTickFeedback(secondsRemaining: Int) {
    if (secondsRemaining <= 3) {
        PlatformSoundPlayer.playHaptic()
    }
}

actual fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?) {
    // iOS Live Activity / Dynamic Island placeholder
}

actual fun notifyRestTimerPaused(remainingSeconds: Int, exerciseName: String?) {
    // iOS pause
}

actual fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int) {
    // iOS update
}

actual fun notifyRestTimerCompleted(exerciseName: String?, targetEndEpochMs: Long) {
    // iOS completion notification placeholder
}

actual fun notifyRestTimerStopped() {
    // iOS stop
}
