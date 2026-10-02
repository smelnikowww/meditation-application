package com.smelnikowww.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays short chimes with a gentle fade-in so the bell never startles.
 * Only one chime sounds at a time; a new one replaces the previous.
 */
class SoundPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var player: MediaPlayer? = null
    private var fadeJob: Job? = null

    fun play(source: SoundSource) {
        release()
        val mediaPlayer = try {
            MediaPlayer.create(context, source.resId)
        } catch (error: Exception) {
            Log.e(TAG, "Cannot create media player", error)
            null
        } ?: return

        mediaPlayer.setAudioAttributes(AUDIO_ATTRIBUTES)
        mediaPlayer.setVolume(0f, 0f)
        mediaPlayer.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "Playback error what=$what extra=$extra")
            release()
            true
        }
        mediaPlayer.start()
        Log.i(TAG, "play ${source.name}")
        player = mediaPlayer
        fadeJob = scope.launch {
            for (step in 1..FADE_STEPS) {
                delay(FADE_MILLIS / FADE_STEPS)
                val volume = step.toFloat() / FADE_STEPS
                runCatching { mediaPlayer.setVolume(volume, volume) }
            }
        }
    }

    fun release() {
        fadeJob?.cancel()
        fadeJob = null
        player?.let { runCatching { it.release() } }
        player = null
    }

    private companion object {
        const val TAG = "TishinaSound"
        const val FADE_MILLIS = 700L
        const val FADE_STEPS = 14

        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
