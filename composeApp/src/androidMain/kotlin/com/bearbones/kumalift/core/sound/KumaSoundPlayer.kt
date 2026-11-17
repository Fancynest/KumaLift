package com.bearbones.kumalift.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.bearbones.kumalift.R
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.SoundVolume
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object KumaSoundPlayer {

    private const val TAG = "KumaSoundPlayer"

    private val soundPoolLock = Any()
    @Volatile
    private var soundPool: SoundPool? = null

    private val soundIdMap = ConcurrentHashMap<SoundEvent, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()

    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private val eventToResource = mapOf(
        SoundEvent.REST_END to R.raw.kumalift_rest_end,
        SoundEvent.TICK to R.raw.kumalift_tick,
        SoundEvent.SET_DONE to R.raw.kumalift_set_done,
        SoundEvent.PR to R.raw.kumalift_pr,
        SoundEvent.WORKOUT_DONE to R.raw.kumalift_workout_done,
        SoundEvent.REMINDER to R.raw.kumalift_reminder,
        SoundEvent.ERROR to R.raw.kumalift_error
    )

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
        val appContext = context.applicationContext ?: context
        backgroundExecutor.execute {
            initializeInternal(appContext)
        }
    }

    private fun initializeInternal(context: Context) {
        synchronized(soundPoolLock) {
            if (soundPool != null) return

            try {
                // AudioAttributes.USAGE_MEDIA ensures audio routes through STREAM_MUSIC (Media Volume),
                // so workout and rest sounds remain audible over speakers/headphones and are not
                // suppressed by muted system touch sounds.
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(audioAttributes)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                        KumaLogger.i(TAG, "SoundPool sample loaded: $sampleId")
                    } else {
                        KumaLogger.w(TAG, "SoundPool sample load failed with status=$status for sampleId=$sampleId")
                    }
                }

                soundPool = pool

                for ((event, resId) in eventToResource) {
                    try {
                        val sampleId = pool.load(context, resId, 1)
                        if (sampleId != 0) {
                            soundIdMap[event] = sampleId
                            KumaLogger.i(TAG, "SoundPool registered $event -> sampleId=$sampleId")
                        }
                    } catch (t: Throwable) {
                        KumaLogger.w(TAG, "SoundPool load failed for $event: ${t.message}")
                    }
                }
            } catch (t: Throwable) {
                KumaLogger.e(TAG, "SoundPool initialization exception", t)
            }
        }
    }

    fun play(context: Context, event: SoundEvent, volume: SoundVolume) {
        val appContext = context.applicationContext ?: context
        val volumeMultiplier = volume.multiplier
        val resId = eventToResource[event]

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { requestTransientDuckFocus(it) }

        val pool = soundPool
        val soundId = soundIdMap[event]
        var playedViaSoundPool = false

        if (pool != null && soundId != null && loadedSoundIds.contains(soundId)) {
            try {
                val streamId = pool.play(
                    soundId,
                    volumeMultiplier,
                    volumeMultiplier,
                    1,
                    0,
                    1.0f
                )
                if (streamId != 0) {
                    playedViaSoundPool = true
                    KumaLogger.i(TAG, "Played $event via SoundPool (streamId=$streamId, vol=$volumeMultiplier)")
                } else {
                    KumaLogger.w(TAG, "SoundPool.play returned 0 for $event; falling back to MediaPlayer")
                }
            } catch (t: Throwable) {
                KumaLogger.w(TAG, "SoundPool.play exception for $event: ${t.message}")
            }
        }

        // Guaranteed fallback if SoundPool is not yet loaded, missing, or failed to start playback
        if (!playedViaSoundPool && resId != null) {
            playFallbackMediaPlayer(appContext, resId, volumeMultiplier)
            if (soundPool == null) {
                initialize(appContext)
            }
        }

        val duration = soundDurationsMs[event] ?: 1000L
        audioManager?.let { scheduleAbandonFocus(it, duration) }
    }

    private fun playFallbackMediaPlayer(context: Context, resId: Int, volumeMultiplier: Float) {
        try {
            val afd = context.resources.openRawResourceFd(resId)
            val mp = MediaPlayer()
            if (afd != null) {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                return
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setVolume(volumeMultiplier, volumeMultiplier)
            mp.setOnCompletionListener {
                try { it.release() } catch (_: Throwable) {}
            }
            mp.setOnErrorListener { it, _, _ ->
                try { it.release() } catch (_: Throwable) {}
                true
            }
            mp.prepare()
            mp.start()
            KumaLogger.i(TAG, "Fallback MediaPlayer played resId=$resId (vol=$volumeMultiplier)")
        } catch (t: Throwable) {
            KumaLogger.e(TAG, "Fallback MediaPlayer failed for resId=$resId", t)
        }
    }

    private var activeFocusRequest: AudioFocusRequest? = null
    private var pendingAbandonRunnable: Runnable? = null

    private fun requestTransientDuckFocus(audioManager: AudioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (t: Throwable) {
            KumaLogger.w(TAG, "Failed to request audio focus: ${t.message}")
        }
    }

    private fun scheduleAbandonFocus(audioManager: AudioManager, durationMs: Long) {
        pendingAbandonRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            abandonFocus(audioManager)
            pendingAbandonRunnable = null
        }
        pendingAbandonRunnable = runnable
        handler.postDelayed(runnable, durationMs + 100L)
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
        } catch (t: Throwable) {
            KumaLogger.w(TAG, "Failed to abandon audio focus: ${t.message}")
        }
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
