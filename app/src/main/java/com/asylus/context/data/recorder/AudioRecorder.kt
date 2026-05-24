package com.asylus.context.data.recorder

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun getMaxAmplitude(): Int
}
