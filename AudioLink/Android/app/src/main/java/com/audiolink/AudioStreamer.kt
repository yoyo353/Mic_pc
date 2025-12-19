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
import org.concentus.OpusEncoder
import org.concentus.OpusApplication
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioStreamer(private val useOpus: Boolean = true) {

    private var audioRecord: AudioRecord? = null
    private var isStreaming = false
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    // Audio Configuration
    private val sampleRate = 48000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val frameSize = 960 // 20ms at 48kHz
    
    // Opus Encoder
    private var opusEncoder: OpusEncoder? = null

    interface StreamListener {
        fun onConnectionOpened()
        fun onConnectionClosed(reason: String)
        fun onConnectionFailed(t: Throwable)
    }
    
    var listener: StreamListener? = null

    init {
        if (useOpus) {
            try {
                // Initialize Opus encoder: 48kHz, 1 channel, VoIP application
                opusEncoder = OpusEncoder(sampleRate, 1, OpusApplication.OPUS_APPLICATION_VOIP)
                opusEncoder?.bitrate = 64000 // 64 kbps
                opusEncoder?.complexity = 10 // Max quality
                Log.i("AudioStreamer", "Opus encoder initialized (64kbps)")
            } catch (e: Exception) {
                Log.e("AudioStreamer", "Failed to initialize Opus encoder: ${e.message}")
            }
        } else {
            Log.i("AudioStreamer", "Running in PCM mode (no Opus)")
        }
    }

    @SuppressLint("MissingPermission") // Checked in Activity
    fun startStreaming(ipAddress: String, port: Int = 8765) {
        if (isStreaming) return

        // Initialize WebSocket
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
                val pcmBuffer = ShortArray(frameSize) // 960 samples
                val opusBuffer = ByteArray(4000) // Max Opus frame size
                
                while (isStreaming) {
                    val read = audioRecord?.read(pcmBuffer, 0, frameSize) ?: 0
                    if (read > 0) {
                        if (useOpus && opusEncoder != null) {
                            try {
                                // Encode PCM to Opus
                                val encodedSize = opusEncoder!!.encode(pcmBuffer, 0, frameSize, opusBuffer, 0, opusBuffer.size)
                                if (encodedSize > 0) {
                                    // Send Opus frame
                                    webSocket?.send(ByteString.of(opusBuffer, 0, encodedSize))
                                }
                            } catch (e: Exception) {
                                Log.e("AudioStreamer", "Opus encoding error: ${e.message}")
                            }
                        } else {
                            // Send raw PCM (convert short[] to byte[])
                            val pcmBytes = shortArrayToByteArray(pcmBuffer)
                            webSocket?.send(ByteString.of(pcmBytes, 0, pcmBytes.size))
                        }
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
