package com.bearbones.kumalift.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.bearbones.kumalift.R
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt

object KumaSoundPlayer {

    private const val TAG = "KumaSoundPlayer"

    private val soundPoolLock = Any()
    @Volatile
    private var soundPool: SoundPool? = null

    private val soundIdMap = ConcurrentHashMap<SoundEvent, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeSoundPoolStreamIds = ConcurrentHashMap.newKeySet<Int>()

    private val handler by lazy { Handler(Looper.getMainLooper()) }
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

    var onCustomSoundCompletion: (() -> Unit)? = null

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
                // AudioAttributes.USAGE_MEDIA routes through STREAM_MUSIC (Media Volume),
                // matching the in-app sound volume slider and ensuring ducking with background music works reliably.
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

    fun play(context: Context, event: SoundEvent, volumePercent: Int) {
        val appContext = context.applicationContext ?: context
        val volumeMultiplier = (volumePercent / 100f).coerceIn(0f, 1f)
        if (volumeMultiplier <= 0f) {
            KumaLogger.d(TAG, "Muted (volume 0%) for $event")
            return
        }
        stopCustomSound()
        stopFallbackPlayer()
        val resId = eventToResource[event]

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            requestTransientDuckFocus(am)
        }

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
                    activeSoundPoolStreamIds.add(streamId)
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

    private var activeFallbackPlayer: MediaPlayer? = null

    private fun playFallbackMediaPlayer(context: Context, resId: Int, volumeMultiplier: Float) {
        try {
            stopFallbackPlayer()
            val afd = context.resources.openRawResourceFd(resId)
            val mp = MediaPlayer()
            if (afd != null) {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                return
            }
            val isAlarm = resId == R.raw.kumalift_rest_end
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setVolume(volumeMultiplier, volumeMultiplier)
            mp.setOnCompletionListener {
                synchronized(customPlayerLock) {
                    if (activeFallbackPlayer === it) {
                        activeFallbackPlayer = null
                    }
                }
                try { it.release() } catch (_: Throwable) {}
            }
            mp.setOnErrorListener { it, _, _ ->
                synchronized(customPlayerLock) {
                    if (activeFallbackPlayer === it) {
                        activeFallbackPlayer = null
                    }
                }
                try { it.release() } catch (_: Throwable) {}
                true
            }
            mp.prepare()
            mp.start()
            synchronized(customPlayerLock) {
                activeFallbackPlayer = mp
            }
            KumaLogger.i(TAG, "Fallback MediaPlayer played resId=$resId (vol=$volumeMultiplier)")
        } catch (t: Throwable) {
            KumaLogger.e(TAG, "Fallback MediaPlayer failed for resId=$resId", t)
        }
    }

    fun stopFallbackPlayer() {
        val player = synchronized(customPlayerLock) {
            val p = activeFallbackPlayer
            activeFallbackPlayer = null
            p
        }
        if (player != null) {
            try { player.stop() } catch (_: Throwable) {}
            try { player.reset() } catch (_: Throwable) {}
            try { player.release() } catch (_: Throwable) {}
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
        stopCustomSound()
        synchronized(soundPoolLock) {
            soundPool?.release()
            soundPool = null
            loadedSoundIds.clear()
            soundIdMap.clear()
        }
    }

    fun play(context: Context, event: SoundEvent, volume: SoundVolume) {
        play(context, event, volume.percent)
    }

    private var activeCustomPlayer: MediaPlayer? = null
    private val customPlayerLock = Any()

    fun playCustomSound(context: Context, filePath: String, volumeMultiplier: Float) {
        stopCustomSound()
        if (volumeMultiplier <= 0f) {
            KumaLogger.d(TAG, "Muted (volume 0%) for custom sound")
            return
        }
        val appContext = context.applicationContext ?: context
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            KumaLogger.w(TAG, "Custom sound file not found or unreadable: $filePath. Falling back to built-in sound.")
            play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
            return
        }

        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                requestTransientDuckFocus(am)
            }

            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(volumeMultiplier, volumeMultiplier)
                setOnCompletionListener {
                    synchronized(customPlayerLock) {
                        if (activeCustomPlayer === it) {
                            activeCustomPlayer = null
                        }
                    }
                    try { it.release() } catch (_: Throwable) {}
                    audioManager?.let { am -> abandonFocus(am) }
                    notifyPlaybackStopped()
                }
                setOnErrorListener { it, what, extra ->
                    KumaLogger.w(TAG, "MediaPlayer custom sound error what=$what extra=$extra. Falling back to built-in.")
                    synchronized(customPlayerLock) {
                        if (activeCustomPlayer === it) {
                            activeCustomPlayer = null
                        }
                    }
                    try { it.release() } catch (_: Throwable) {}
                    audioManager?.let { am -> abandonFocus(am) }
                    notifyPlaybackStopped()
                    play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
                    true
                }
                prepare()
                start()
            }
            synchronized(customPlayerLock) {
                activeCustomPlayer = mp
            }
            KumaLogger.i(TAG, "Playing custom sound from $filePath at volume $volumeMultiplier")
        } catch (e: Exception) {
            KumaLogger.e(TAG, "Failed to play custom sound from $filePath: ${e.message}. Falling back.", e)
            stopCustomSound()
            play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
        }
    }

    fun playSystemDefaultSound(context: Context, volumeMultiplier: Float) {
        stopCustomSound()
        if (volumeMultiplier <= 0f) {
            KumaLogger.d(TAG, "Muted (volume 0%) for system default sound")
            return
        }
        val appContext = context.applicationContext ?: context
        try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (defaultUri == null) {
                play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
                return
            }

            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                requestTransientDuckFocus(am)
            }

            val mp = MediaPlayer().apply {
                setDataSource(appContext, defaultUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(volumeMultiplier, volumeMultiplier)
                setOnCompletionListener {
                    synchronized(customPlayerLock) {
                        if (activeCustomPlayer === it) {
                            activeCustomPlayer = null
                        }
                    }
                    try { it.release() } catch (_: Throwable) {}
                    audioManager?.let { am -> abandonFocus(am) }
                    notifyPlaybackStopped()
                }
                setOnErrorListener { it, _, _ ->
                    synchronized(customPlayerLock) {
                        if (activeCustomPlayer === it) {
                            activeCustomPlayer = null
                        }
                    }
                    try { it.release() } catch (_: Throwable) {}
                    audioManager?.let { am -> abandonFocus(am) }
                    notifyPlaybackStopped()
                    play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
                    true
                }
                prepare()
                start()
            }
            synchronized(customPlayerLock) {
                activeCustomPlayer = mp
            }
        } catch (e: Exception) {
            KumaLogger.e(TAG, "Failed to play system default sound: ${e.message}", e)
            play(appContext, SoundEvent.REST_END, (volumeMultiplier * 100).toInt())
        }
    }

    private fun notifyPlaybackStopped() {
        try {
            handler.post {
                onCustomSoundCompletion?.invoke()
            }
        } catch (_: Throwable) {
            onCustomSoundCompletion?.invoke()
        }
    }

    fun playRestTimerSound(
        context: Context,
        volumePercent: Int,
        source: RestTimerSoundSource,
        customPath: String?
    ) {
        val volumeMultiplier = (volumePercent / 100f).coerceIn(0f, 1f)
        if (volumeMultiplier <= 0f) {
            KumaLogger.d(TAG, "Muted (volume 0%) for rest timer sound")
            return
        }
        when (source) {
            RestTimerSoundSource.CUSTOM -> {
                if (!customPath.isNullOrBlank()) {
                    playCustomSound(context, customPath, volumeMultiplier)
                } else {
                    play(context, SoundEvent.REST_END, volumePercent)
                }
            }
            RestTimerSoundSource.SYSTEM_DEFAULT -> {
                playSystemDefaultSound(context, volumeMultiplier)
            }
            RestTimerSoundSource.BUILTIN -> {
                play(context, SoundEvent.REST_END, volumePercent)
            }
        }
    }

    fun playRestTimerSound(
        context: Context,
        volume: SoundVolume,
        source: RestTimerSoundSource,
        customPath: String?
    ) {
        playRestTimerSound(context, volume.percent, source, customPath)
    }

    fun stopCustomSound() {
        val player = synchronized(customPlayerLock) {
            val p = activeCustomPlayer
            activeCustomPlayer = null
            p
        }
        if (player != null) {
            try { player.stop() } catch (_: Throwable) {}
            try { player.reset() } catch (_: Throwable) {}
            try { player.release() } catch (_: Throwable) {}
        }
        val ctx = AndroidAppProvider.appContext
        val audioManager = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { abandonFocus(it) }
    }

    fun stopAllSounds(context: Context? = null) {
        stopCustomSound()
        stopFallbackPlayer()
        synchronized(soundPoolLock) {
            try {
                soundPool?.let { pool ->
                    activeSoundPoolStreamIds.forEach { sId ->
                        try { pool.stop(sId) } catch (_: Throwable) {}
                    }
                    activeSoundPoolStreamIds.clear()
                    pool.autoPause()
                }
            } catch (_: Throwable) {}
        }
        val ctx = context ?: AndroidAppProvider.appContext
        val audioManager = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { abandonFocus(it) }
    }
}
