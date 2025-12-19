package com.audiolink

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

class AudioStreamer {

    private var audioRecord: AudioRecord? = null
    private var isStreaming = false
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    // Audio Configuration
    private val sampleRate = 48000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    interface StreamListener {
        fun onConnectionOpened()
        fun onConnectionClosed(reason: String)
        fun onConnectionFailed(t: Throwable)
    }
    
    var listener: StreamListener? = null

    @SuppressLint("MissingPermission") // Checked in Activity
    fun startStreaming(ipAddress: String, port: Int = 8765) {
        if (isStreaming) return

        // 1. Initialize WebSocket
        val request = Request.Builder().url("ws://$ipAddress:$port").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("AudioStreamer", "Connected to server")
                listener?.onConnectionOpened()
                startAudioCapture()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("AudioStreamer", "Connection closed: $reason")
                stopAudioCapture()
                listener?.onConnectionClosed(reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("AudioStreamer", "Connection failed", t)
                stopAudioCapture()
                listener?.onConnectionFailed(t)
            }
        })
    }

    private fun startAudioCapture() {
        if (isStreaming) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioStreamer", "AudioRecord not initialized")
                return
            }

            audioRecord?.startRecording()
            isStreaming = true

            Thread {
                val buffer = ByteArray(960 * 2) // 20ms chunk at 48kHz 16-bit
                while (isStreaming) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Send binary data
                        webSocket?.send(ByteString.of(buffer, 0, read))
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error starting capture: ${e.message}")
            stopStreaming()
        }
    }

    private fun stopAudioCapture() {
        isStreaming = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }

    fun stopStreaming() {
        isStreaming = false
        webSocket?.close(1000, "User stopped")
        webSocket = null
        stopAudioCapture()
    }
}
