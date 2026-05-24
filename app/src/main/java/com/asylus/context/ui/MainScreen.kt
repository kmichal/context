package com.asylus.context.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asylus.context.ui.screens.PlayerScreen
import com.asylus.context.ui.screens.RecordingHomeScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedRecording by viewModel.selectedRecordingForPlayback.collectAsState()
    val isPlaybackPlaying by viewModel.isPlaybackPlaying.collectAsState()

    if (selectedRecording != null) {
        PlayerScreen(
            recording = selectedRecording!!,
            isPlaying = isPlaybackPlaying,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onDeleteClick = { viewModel.deleteRecording(selectedRecording!!) },
            onBackClick = { viewModel.closePlayer() }
        )
    } else {
        RecordingHomeScreen(viewModel = viewModel)
    }
}
