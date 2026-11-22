package com.tiwgo.pongreimaginado

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val bounceSoundId: Int
    private val powerUpSoundId: Int
    private val punishmentSoundId: Int

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()

        bounceSoundId = soundPool.load(context, R.raw.bolinha_quicando, 1)
        powerUpSoundId = soundPool.load(context, R.raw.power_up, 1)
        punishmentSoundId = soundPool.load(context, R.raw.zona_punicao, 1)
    }

    fun playBounce() {
        soundPool.play(bounceSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playPowerUp() {
        soundPool.play(powerUpSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playPunishment() {
        soundPool.play(punishmentSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun startOrResumeMusic() {
        Companion.startOrResumeMusic(context)
    }

    fun pauseMusic(userInitiated: Boolean = false) {
        Companion.pauseMusic(userInitiated)
    }

    fun setPausedByGameToggle(paused: Boolean) {
        Companion.setPausedByGameToggle(paused)
    }

    fun releaseEffects() {
        soundPool.release()
    }

    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var isMusicPausedByUser: Boolean = false
        private var activeScreenCount: Int = 0
        private val mainHandler = Handler(Looper.getMainLooper())
        private var pendingPauseRunnable: Runnable? = null

        private fun ensureMediaPlayer(context: Context): MediaPlayer {
            return mediaPlayer ?: MediaPlayer.create(context.applicationContext, R.raw.musica_fundo).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                mediaPlayer = this
            }
        }

        fun onScreenResume(context: Context) {
            pendingPauseRunnable?.let { mainHandler.removeCallbacks(it) }
            activeScreenCount++
            startOrResumeMusic(context)
        }

        fun onScreenPause() {
            activeScreenCount = (activeScreenCount - 1).coerceAtLeast(0)
            pendingPauseRunnable?.let { mainHandler.removeCallbacks(it) }
            val runnable = Runnable {
                if (activeScreenCount == 0) {
                    pauseMusic(userInitiated = false)
                }
            }
            pendingPauseRunnable = runnable
            mainHandler.postDelayed(runnable, 250L)
        }

        fun startOrResumeMusic(context: Context) {
            if (isMusicPausedByUser) return
            val player = ensureMediaPlayer(context)
            if (!player.isPlaying) {
                player.start()
            }
        }

        fun pauseMusic(userInitiated: Boolean = false) {
            if (userInitiated) {
                isMusicPausedByUser = true
            }
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }

        fun setPausedByGameToggle(paused: Boolean) {
            if (paused) {
                pauseMusic(userInitiated = true)
            } else {
                isMusicPausedByUser = false
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            }
        }
    }
}