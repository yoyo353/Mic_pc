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
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioStreamer(private val useOpus: Boolean = false) {  // Default to PCM mode

    private var audioRecord: AudioRecord? = null
    private var isStreaming = false
    private var webSocket: WebSocket? = null
    
    // Optimized OkHttp client for WiFi 5GHz low latency
    private val client = OkHttpClient.Builder()
        .pingInterval(5, java.util.concurrent.TimeUnit.SECONDS)  // Keep connection alive
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)  // Fast timeout detection
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)  // Auto-retry on network issues
        .build()

    // Audio Configuration
    private val sampleRate = 48000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val frameSize = 960 // 20ms at 48kHz

    interface StreamListener {
        fun onConnectionOpened()
        fun onConnectionClosed(reason: String)
        fun onConnectionFailed(t: Throwable)
    }
    
    var listener: StreamListener? = null

    init {
        // PCM mode only (no Opus) - matching server's --pcm mode
        Log.i("AudioStreamer", "Running in PCM mode (no Opus compression)")
        Log.i("AudioStreamer", "Note: This uses more bandwidth but works without Opus library")
    }

    @SuppressLint("MissingPermission") // Checked in Activity
    fun startStreaming(ipAddress: String, port: Int = 8765) {
        if (isStreaming) return

        // Initialize WebSocket
        val request = Request.Builder().url("ws://$ipAddress:$port").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("AudioStreamer", "Connected to server")
                Log.i("AudioStreamer", "========================================")
                Log.i("AudioStreamer", "WiFi 5GHz Optimization: ENABLED")
                Log.i("AudioStreamer", "For best performance:")
                Log.i("AudioStreamer", "  • Use WiFi 5GHz (not 2.4GHz)")
                Log.i("AudioStreamer", "  • Stay close to router")
                Log.i("AudioStreamer", "  • Expected latency: 90-115ms")
                Log.i("AudioStreamer", "========================================")
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
                val pcmBuffer = ShortArray(frameSize) // 960 samples
                
                while (isStreaming) {
                    val read = audioRecord?.read(pcmBuffer, 0, frameSize) ?: 0
                    if (read > 0) {
                        // Send raw PCM (convert short[] to byte[])
                        val pcmBytes = shortArrayToByteArray(pcmBuffer)
                        webSocket?.send(ByteString.of(pcmBytes, 0, pcmBytes.size))
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error starting capture: ${e.message}")
            stopStreaming()
        }
    }

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (s in shorts) {
            buffer.putShort(s)
        }
        return bytes
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
