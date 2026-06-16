package com.asylus.context.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asylus.context.ui.navigation.Screen
import com.asylus.context.ui.screens.PlayerScreen
import com.asylus.context.ui.screens.RecordingHomeScreen
import com.asylus.context.ui.screens.SettingsScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedRecording by viewModel.selectedRecordingForPlayback.collectAsState()
    val isPlaybackPlaying by viewModel.isPlaybackPlaying.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    when {
        selectedRecording != null -> {
            PlayerScreen(
                recording = selectedRecording!!,
                isPlaying = isPlaybackPlaying,
                isTranscribing = isTranscribing,
                liveTranscript = transcript,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onDeleteClick = { viewModel.deleteRecording(selectedRecording!!) },
                onTranscribeClick = { viewModel.transcribeRecording(selectedRecording!!) },
                onBackClick = { viewModel.closePlayer() }
            )
        }
        currentScreen == Screen.Settings -> {
            SettingsScreen(viewModel = viewModel, onBackClick = { viewModel.navigateTo(Screen.Home) })
        }
        else -> {
            RecordingHomeScreen(viewModel = viewModel)
        }
    }
}
