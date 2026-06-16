package com.asylus.context.data.transcription

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class XaiAudioTranscriber(private val apiKeyProvider: () -> String) : AudioTranscriber {
    private val _transcript = MutableStateFlow("")
    override val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    override val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    override fun startLiveTranscription() {
        _transcript.value = "Live transcription not supported with xAI yet."
    }

    override fun stopLiveTranscription() {
        // No-op
    }

    override fun transcribeFile(file: File) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            _transcript.value = "Error: xAI API Key is missing in Settings"
            return
        }

        _isTranscribing.value = true
        _transcript.value = ""

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/mp4".toMediaType())
            )
            .addFormDataPart("model", "grok-stt")
            .addFormDataPart("format", "true")
            .addFormDataPart("language", "en")
            .build()

        val request = Request.Builder()
            .url("https://api.x.ai/v1/stt")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _transcript.value = "Transcription failed: ${e.message}"
                _isTranscribing.value = false
                Log.e("XaiTranscriber", "Failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        val text = json.optString("text", "")
                        _transcript.value = text
                    } catch (e: Exception) {
                        _transcript.value = "Error parsing response"
                    }
                } else {
                    try {
                        val errorJson = JSONObject(body ?: "{}")
                        val errorMsg = errorJson.optString("error", "Unknown API error")
                        _transcript.value = "API Error: $errorMsg"
                    } catch (e: Exception) {
                        _transcript.value = "API Error: ${response.code}"
                    }
                    Log.e("XaiTranscriber", "Error Body: $body")
                }
                _isTranscribing.value = false
            }
        })
    }

    override fun clearTranscript() {
        _transcript.value = ""
    }
}
