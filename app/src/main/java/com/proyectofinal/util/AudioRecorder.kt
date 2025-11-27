package com.proyectofinal.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    // La ruta absoluta donde guardaremos el archivo
    fun start(absolutePath: String) {

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Uso del constructor con Context para API 31+
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            // Usamos formato y codificador modernos compatibles
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(absolutePath) // 👈 Aquí se guarda directamente

            try {
                prepare()
                start()
            } catch (e: IOException) {
                // Limpiar y lanzar excepción si falla la preparación
                release()
                throw e
            }
        }
    }

    fun stop() {
        try {
            // Importante llamar stop() para finalizar el archivo
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Manejar excepciones si stop se llama incorrectamente
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    val isRecording: Boolean
        get() = recorder != null
}