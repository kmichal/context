package com.asylus.context.data.transcription

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class AndroidAudioTranscriber(private val context: Context) : AudioTranscriber, RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _transcript = MutableStateFlow("")
    override val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    override val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private var currentText = ""
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startLiveTranscription() {
        isListening = true
        _isTranscribing.value = true
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(this)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                
                _transcript.value = if (currentText.isEmpty()) "Connecting..." else currentText
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("AudioTranscriber", "Error", e)
                _transcript.value = "System Error: ${e.message}"
            }
        }
    }

    override fun stopLiveTranscription() {
        isListening = false
        _isTranscribing.value = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("AudioTranscriber", "Stop Error", e)
            }
            speechRecognizer = null
        }
    }

    override fun transcribeFile(file: File) {
        _transcript.value = "File transcription not supported with Android System engine."
    }

    override fun clearTranscript() {
        currentText = ""
        _transcript.value = ""
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        if (isListening) {
            // Self-heal and restart
            mainHandler.postDelayed({
                if (isListening) startLiveTranscription()
            }, 800)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val result = matches[0]
            currentText = if (currentText.isEmpty()) result else "$currentText $result"
            _transcript.value = currentText
        }
        if (isListening) startLiveTranscription()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0]
            _transcript.value = if (currentText.isEmpty()) partial else "$currentText $partial"
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
