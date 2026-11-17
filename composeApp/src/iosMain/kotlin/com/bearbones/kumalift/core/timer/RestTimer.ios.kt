package com.bearbones.kumalift.core.timer

actual fun playTimerCompletionFeedback() {
    // iOS platform timer feedback
}

actual fun playTimerTickFeedback(secondsRemaining: Int) {
    // iOS platform timer tick
}

actual fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?) {
    // iOS Live Activity / Dynamic Island placeholder
}

actual fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int) {
    // iOS update
}

actual fun notifyRestTimerStopped() {
    // iOS stop
}
