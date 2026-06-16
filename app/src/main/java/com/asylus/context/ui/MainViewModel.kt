package com.asylus.context.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.asylus.context.R
import androidx.lifecycle.viewModelScope
import com.asylus.context.data.model.Recording
import com.asylus.context.data.player.AndroidAudioPlayer
import com.asylus.context.data.recorder.AndroidAudioRecorder
import com.asylus.context.data.repository.RecordingRepository
import com.asylus.context.data.transcription.AndroidAudioTranscriber
import com.asylus.context.data.transcription.AudioTranscriber
import com.asylus.context.data.transcription.XaiAudioTranscriber
import com.asylus.context.ui.navigation.Screen
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

    private val appContext = application
    
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        "context_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val recorder = AndroidAudioRecorder(appContext)
    private val player = AndroidAudioPlayer(appContext)
    private val repository = RecordingRepository(appContext)
    
    // Transcribers
    private val xaiTranscriber = XaiAudioTranscriber { prefs.getString("xai_api_key", "")?.trim() ?: "" }
    private val androidTranscriber = AndroidAudioTranscriber(appContext)
    
    private var activeTranscriber: AudioTranscriber = xaiTranscriber

    private var activeFile: File? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _elapsedTime = MutableStateFlow(appContext.getString(R.string.default_duration))
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

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    init {
        loadRecordings()
        updateEngine()
        
        viewModelScope.launch {
            launch {
                xaiTranscriber.transcript.collect { if (activeTranscriber == xaiTranscriber) _transcript.value = it }
            }
            launch {
                androidTranscriber.transcript.collect { if (activeTranscriber == androidTranscriber) _transcript.value = it }
            }
            launch {
                xaiTranscriber.isTranscribing.collect { if (activeTranscriber == xaiTranscriber) handleTranscriptionStateChange(it) }
            }
            launch {
                androidTranscriber.isTranscribing.collect { if (activeTranscriber == androidTranscriber) handleTranscriptionStateChange(it) }
            }
        }
    }

    private fun handleTranscriptionStateChange(isCurrentlyTranscribing: Boolean) {
        val wasTranscribing = _isTranscribing.value
        _isTranscribing.value = isCurrentlyTranscribing

        // If we just finished transcribing (transition from true to false), save the result
        if (wasTranscribing && !isCurrentlyTranscribing) {
            val finalTranscript = _transcript.value
            if (finalTranscript.isNotEmpty() && !finalTranscript.startsWith("API Error") && !finalTranscript.startsWith("Transcription failed")) {
                activeFile?.let { file ->
                    repository.saveTranscript(file, finalTranscript)
                    loadRecordings()
                    
                    // Update selected recording if it's the one we just transcribed
                    if (_selectedRecordingForPlayback.value?.file == file) {
                        _selectedRecordingForPlayback.value = _selectedRecordingForPlayback.value?.copy(transcript = finalTranscript)
                    }
                }
            }
        }
    }

    private fun updateEngine() {
        val engine = prefs.getString("selected_engine", "xAI (Grok)")
        activeTranscriber = if (engine == "xAI (Grok)") xaiTranscriber else androidTranscriber
    }

    fun setXaiApiKey(key: String) {
        prefs.edit { putString("xai_api_key", key) }
    }

    fun getXaiApiKey(): String = prefs.getString("xai_api_key", "") ?: ""

    fun setSelectedEngine(engine: String) {
        prefs.edit { putString("selected_engine", engine) }
        updateEngine()
    }

    fun getSelectedEngine(): String = prefs.getString("selected_engine", "xAI (Grok)") ?: "xAI (Grok)"

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
        stopPlayback()
        val file = repository.createRecordingFile()
        activeFile = file
        
        activeTranscriber.clearTranscript()
        if (activeTranscriber == androidTranscriber) {
            activeTranscriber.startLiveTranscription()
        }

        viewModelScope.launch {
            delay(300) 
            recorder.start(file)
        }

        _isRecording.value = true
        _elapsedTime.value = appContext.getString(R.string.default_duration)
        startTimer()
        startAmplitudePolling()
    }

    private fun stopRecording() {
        recorder.stop()
        if (activeTranscriber == androidTranscriber) {
            activeTranscriber.stopLiveTranscription()
        } else {
            activeFile?.let { activeTranscriber.transcribeFile(it) }
        }
        
        _isRecording.value = false
        _amplitudeScale.value = 1.0f
        stopTimer()
        stopAmplitudePolling()
        loadRecordings()
    }

    fun transcribeRecording(recording: Recording) {
        activeFile = recording.file
        activeTranscriber.clearTranscript()
        activeTranscriber.transcribeFile(recording.file)
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMs = System.currentTimeMillis() - startTime
                val seconds = elapsedMs / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                _elapsedTime.value = String.format(Locale.getDefault(), appContext.getString(R.string.duration_format), minutes, remainingSeconds)
                delay(200)
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
                val ratio = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                _amplitudeScale.value = 1.0f + ratio * 0.5f
                delay(70)
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
        _transcript.value = recording.transcript ?: ""
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
        _transcript.value = ""
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

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
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
