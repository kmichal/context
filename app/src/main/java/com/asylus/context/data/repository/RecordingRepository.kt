package com.asylus.context.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
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

        // Sort by last modified descending (newest first)
        return files.sortedByDescending { it.lastModified() }.map { file ->
            val timestamp = file.lastModified()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val displayName = "Recording $formattedDate"

            Recording(
                file = file,
                displayName = displayName,
                durationFormatted = getDurationFormatted(file),
                timestamp = timestamp
            )
        }
    }

    fun deleteRecording(recording: Recording): Boolean {
        return try {
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
            "00:00"
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
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
