package com.asylus.context.data.player

import java.io.File

interface AudioPlayer {
    fun play(file: File, onComplete: () -> Unit)
    fun stop()
    fun isPlaying(): Boolean
}
