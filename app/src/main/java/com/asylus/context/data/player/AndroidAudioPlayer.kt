package com.asylus.context.data.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    override fun play(file: File, onComplete: () -> Unit) {
        stop()

        val player = MediaPlayer.create(context, Uri.fromFile(file))
        if (player != null) {
            mediaPlayer = player.apply {
                setOnCompletionListener {
                    stop()
                    onComplete()
                }
                try {
                    start()
                } catch (e: Exception) {
                    Log.e("AndroidAudioPlayer", "Failed to start playback", e)
                    stop()
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    override fun stop() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (e: Exception) {
                Log.e("AndroidAudioPlayer", "Failed to stop mediaPlayer", e)
            }
            release()
        }
        mediaPlayer = null
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }
}
