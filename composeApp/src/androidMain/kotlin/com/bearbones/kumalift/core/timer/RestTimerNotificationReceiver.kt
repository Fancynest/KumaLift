package com.bearbones.kumalift.core.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REST_TIMER_ADD_30 -> {
                RestTimerCoordinator.addSeconds(30)
            }
            ACTION_REST_TIMER_SKIP -> {
                RestTimerCoordinator.skip()
            }
        }
    }

    companion object {
        const val ACTION_REST_TIMER_ADD_30 = "com.bearbones.kumalift.ACTION_REST_TIMER_ADD_30"
        const val ACTION_REST_TIMER_SKIP = "com.bearbones.kumalift.ACTION_REST_TIMER_SKIP"
    }
}
