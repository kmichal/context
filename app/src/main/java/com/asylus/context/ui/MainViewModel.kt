package com.asylus.context.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asylus.context.data.model.Recording
import com.asylus.context.data.player.AndroidAudioPlayer
import com.asylus.context.data.recorder.AndroidAudioRecorder
import com.asylus.context.data.repository.RecordingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val recorder = AndroidAudioRecorder(application)
    private val player = AndroidAudioPlayer(application)
    private val repository = RecordingRepository(application)

    private var activeFile: File? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedTime = MutableStateFlow("00:00")
    val elapsedTime: StateFlow<String> = _elapsedTime.asStateFlow()

    private val _amplitudeScale = MutableStateFlow(1.0f)
    val amplitudeScale: StateFlow<Float> = _amplitudeScale.asStateFlow()

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<Recording?>(null)
    val currentlyPlaying: StateFlow<Recording?> = _currentlyPlaying.asStateFlow()

    private val _selectedRecordingForPlayback = MutableStateFlow<Recording?>(null)
    val selectedRecordingForPlayback: StateFlow<Recording?> = _selectedRecordingForPlayback.asStateFlow()

    private val _isPlaybackPlaying = MutableStateFlow(false)
    val isPlaybackPlaying: StateFlow<Boolean> = _isPlaybackPlaying.asStateFlow()

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        viewModelScope.launch {
            try {
                _recordings.value = repository.getRecordings()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load recordings", e)
            }
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        // Stop any active playback when starting a recording
        stopPlayback()

        val file = repository.createRecordingFile()
        activeFile = file
        recorder.start(file)
        _isRecording.value = true
        _elapsedTime.value = "00:00"

        startTimer()
        startAmplitudePolling()
    }

    private fun stopRecording() {
        recorder.stop()
        _isRecording.value = false
        _amplitudeScale.value = 1.0f

        stopTimer()
        stopAmplitudePolling()

        // Reload recordings to include the newly saved recording
        loadRecordings()
        activeFile = null
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMs = System.currentTimeMillis() - startTime
                val seconds = elapsedMs / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                _elapsedTime.value = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
                delay(200) // Update 5 times a second for snappy response
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startAmplitudePolling() {
        amplitudeJob = viewModelScope.launch {
            while (isActive) {
                val maxAmp = recorder.getMaxAmplitude()
                // Map maxAmp (0 to 32767) to a scale factor (1.0f to 1.5f)
                val ratio = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                _amplitudeScale.value = 1.0f + ratio * 0.5f
                delay(70) // Poll ~14 times per second for smooth wave responsiveness
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitudeScale.value = 1.0f
    }

    fun selectRecording(recording: Recording) {
        _selectedRecordingForPlayback.value = recording
        startPlayback(recording)
    }

    private fun startPlayback(recording: Recording) {
        stopPlayback()
        _currentlyPlaying.value = recording
        _isPlaybackPlaying.value = true
        player.play(recording.file) {
            _isPlaybackPlaying.value = false
            _currentlyPlaying.value = null
        }
    }

    fun togglePlayPause() {
        val recording = _selectedRecordingForPlayback.value ?: return
        if (_currentlyPlaying.value == null) {
            startPlayback(recording)
        } else {
            if (_isPlaybackPlaying.value) {
                player.pause()
                _isPlaybackPlaying.value = false
            } else {
                player.resume()
                _isPlaybackPlaying.value = true
            }
        }
    }

    fun closePlayer() {
        stopPlayback()
        _selectedRecordingForPlayback.value = null
    }

    fun stopPlayback() {
        if (_currentlyPlaying.value != null) {
            player.stop()
            _currentlyPlaying.value = null
            _isPlaybackPlaying.value = false
        }
    }

    fun deleteRecording(recording: Recording) {
        if (_currentlyPlaying.value == recording) {
            stopPlayback()
        }
        if (_selectedRecordingForPlayback.value == recording) {
            _selectedRecordingForPlayback.value = null
        }
        val deleted = repository.deleteRecording(recording)
        if (deleted) {
            loadRecordings()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            recorder.stop()
        }
        player.stop()
        stopTimer()
        stopAmplitudePolling()
    }
}
