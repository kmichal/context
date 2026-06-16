package com.asylus.context.data.transcription

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AudioTranscriber {
    val transcript: StateFlow<String>
    val isTranscribing: StateFlow<Boolean>
    
    fun startLiveTranscription()
    fun stopLiveTranscription()
    fun transcribeFile(file: File)
    fun clearTranscript()
}
