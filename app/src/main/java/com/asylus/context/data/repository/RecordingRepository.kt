package com.asylus.context.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.asylus.context.R
import com.asylus.context.data.model.Recording
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(private val context: Context) {
    private val recordingsDir: File by lazy {
        File(context.filesDir, "recordings").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun createRecordingFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(recordingsDir, "recording_$timestamp.m4a")
    }

    fun getRecordings(): List<Recording> {
        val files = recordingsDir.listFiles { file ->
            file.isFile && file.name.startsWith("recording_") && file.name.endsWith(".m4a")
        } ?: emptyArray()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val timestamp = file.lastModified()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val displayName = context.getString(R.string.recording_name_format, formattedDate)

            Recording(
                file = file,
                displayName = displayName,
                durationFormatted = getDurationFormatted(file),
                timestamp = timestamp,
                transcript = getTranscript(file)
            )
        }
    }

    fun saveTranscript(audioFile: File, transcript: String) {
        try {
            val transcriptFile = File(audioFile.absolutePath.replace(".m4a", ".txt"))
            transcriptFile.writeText(transcript)
        } catch (e: Exception) {
            Log.e("RecordingRepository", "Failed to save transcript", e)
        }
    }

    private fun getTranscript(audioFile: File): String? {
        val transcriptFile = File(audioFile.absolutePath.replace(".m4a", ".txt"))
        return if (transcriptFile.exists()) {
            transcriptFile.readText()
        } else {
            null
        }
    }

    fun deleteRecording(recording: Recording): Boolean {
        return try {
            val transcriptFile = File(recording.file.absolutePath.replace(".m4a", ".txt"))
            if (transcriptFile.exists()) {
                transcriptFile.delete()
            }
            if (recording.file.exists()) {
                recording.file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("RecordingRepository", "Failed to delete recording file", e)
            false
        }
    }

    private fun getDurationFormatted(file: File): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLong() ?: 0L
            formatDuration(durationMs)
        } catch (e: Exception) {
            context.getString(R.string.default_duration)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), context.getString(R.string.duration_format), minutes, seconds)
    }
}
