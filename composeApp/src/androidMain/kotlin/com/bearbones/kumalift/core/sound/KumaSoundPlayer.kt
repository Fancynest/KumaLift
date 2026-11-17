package com.bearbones.kumalift.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.bearbones.kumalift.R
import com.bearbones.kumalift.domain.model.SoundVolume
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object KumaSoundPlayer {

    private val soundPoolLock = Any()
    @Volatile
    private var soundPool: SoundPool? = null

    private val soundIdMap = ConcurrentHashMap<SoundEvent, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()

    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private val soundDurationsMs = mapOf(
        SoundEvent.REST_END to 1000L,
        SoundEvent.TICK to 120L,
        SoundEvent.SET_DONE to 150L,
        SoundEvent.PR to 1400L,
        SoundEvent.WORKOUT_DONE to 1800L,
        SoundEvent.REMINDER to 1200L,
        SoundEvent.ERROR to 500L
    )

    fun initialize(context: Context) {
        backgroundExecutor.execute {
            synchronized(soundPoolLock) {
                if (soundPool != null) return@execute

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                    }
                }

                soundPool = pool

                // Load all 7 audio assets asynchronously
                val eventToResource = mapOf(
                    SoundEvent.REST_END to R.raw.kumalift_rest_end,
                    SoundEvent.TICK to R.raw.kumalift_tick,
                    SoundEvent.SET_DONE to R.raw.kumalift_set_done,
                    SoundEvent.PR to R.raw.kumalift_pr,
                    SoundEvent.WORKOUT_DONE to R.raw.kumalift_workout_done,
                    SoundEvent.REMINDER to R.raw.kumalift_reminder,
                    SoundEvent.ERROR to R.raw.kumalift_error
                )

                for ((event, resId) in eventToResource) {
                    try {
                        val sampleId = pool.load(context, resId, 1)
                        soundIdMap[event] = sampleId
                    } catch (_: Throwable) {
                        // Silently handle if missing/unreadable
                    }
                }
            }
        }
    }

    fun play(context: Context, event: SoundEvent, volume: SoundVolume) {
        val pool = soundPool ?: return
        val soundId = soundIdMap[event] ?: return
        if (!loadedSoundIds.contains(soundId)) {
            // Silently skip if not loaded yet - no crash, no retry loop
            return
        }

        val volumeMultiplier = volume.multiplier
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        // Request transient ducking audio focus
        val focusAbandonRunnable = Runnable {
            abandonFocus(audioManager)
        }

        requestTransientDuckFocus(audioManager)

        try {
            pool.play(
                soundId,
                volumeMultiplier, // left volume
                volumeMultiplier, // right volume
                1,                // priority
                0,                // loop: 0 = no loop
                1.0f              // playback rate
            )
        } catch (_: Throwable) {
            abandonFocus(audioManager)
            return
        }

        val duration = soundDurationsMs[event] ?: 1000L
        handler.postDelayed(focusAbandonRunnable, duration + 100L)
    }

    private var activeFocusRequest: AudioFocusRequest? = null

    private fun requestTransientDuckFocus(audioManager: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* Ducking handled by system */ }
                    .build()

                activeFocusRequest = focusRequest
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (_: Throwable) {}
    }

    private fun abandonFocus(audioManager: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        synchronized(soundPoolLock) {
            soundPool?.release()
            soundPool = null
            loadedSoundIds.clear()
            soundIdMap.clear()
        }
    }
}
