package com.asylus.context.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null

    override fun start(outputFile: File) {
        if (mediaRecorder != null) return

        mediaRecorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("AndroidAudioRecorder", "Failed to start recording", e)
            }
        }
    }

    override fun stop() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.e("AndroidAudioRecorder", "Failed to stop mediaRecorder", e)
            }
            reset()
            release()
        }
        mediaRecorder = null
    }

    override fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
}
