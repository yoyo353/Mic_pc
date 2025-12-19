package com.audiolink

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var audioStreamer: AudioStreamer
    private lateinit var etIpAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etIpAddress = findViewById(R.id.etIpAddress)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)

        audioStreamer = AudioStreamer()
        audioStreamer.listener = object : AudioStreamer.StreamListener {
            override fun onConnectionOpened() {
                runOnUiThread {
                    isConnected = true
                    tvStatus.text = "Status: Streaming"
                    btnConnect.text = "Disconnect"
                }
            }

            override fun onConnectionClosed(reason: String) {
                runOnUiThread {
                    isConnected = false
                    tvStatus.text = "Status: Disconnected"
                    btnConnect.text = "Connect"
                }
            }

            override fun onConnectionFailed(t: Throwable) {
                runOnUiThread {
                    isConnected = false
                    tvStatus.text = "Status: Error - ${t.message}"
                    btnConnect.text = "Connect"
                }
            }
        }

        btnConnect.setOnClickListener {
            if (isConnected) {
                audioStreamer.stopStreaming()
            } else {
                if (checkPermissions()) {
                    val ip = etIpAddress.text.toString()
                    if (ip.isNotEmpty()) {
                        tvStatus.text = "Status: Connecting..."
                        audioStreamer.startStreaming(ip)
                    } else {
                        Toast.makeText(this, "Enter IP Address", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    requestPermissions()
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET),
            100
        )
    }
}
