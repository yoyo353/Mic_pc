package com.audiolink

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etIpAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var sbVolume: SeekBar
    private lateinit var btnMute: ToggleButton

    private var audioService: AudioService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioService.LocalBinder
            audioService = binder.getService()
            isBound = true

            // Sync UI with current Service state
            audioService?.let {
                updateUiState(it.isStreaming)
                // Re-attach listener to get updates
                it.streamListener = object : AudioStreamer.StreamListener {
                    override fun onConnectionOpened() {
                        runOnUiThread { updateUiState(true) }
                    }

                    override fun onConnectionClosed(reason: String) {
                        runOnUiThread {
                            updateUiState(false)
                            tvStatus.text = "Status: Disconnected"
                        }
                    }

                    override fun onConnectionFailed(t: Throwable) {
                        runOnUiThread {
                            updateUiState(false)
                            tvStatus.text = "Status: Error - ${t.message}"
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            audioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etIpAddress = findViewById(R.id.etIpAddress)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        sbVolume = findViewById(R.id.sbVolume)
        btnMute = findViewById(R.id.btnMute)

        btnConnect.setOnClickListener {
            if (isBound && audioService != null) {
                if (audioService!!.isStreaming) {
                    audioService?.stopStreaming()
                } else {
                    if (checkPermissions()) {
                        val ip = etIpAddress.text.toString()
                        if (ip.isNotEmpty()) {
                            tvStatus.text = "Status: Connecting..."
                            // Start service to ensure it promotes to foreground
                            val intent = Intent(this, AudioService::class.java)
                            ContextCompat.startForegroundService(this, intent)
                            audioService?.startStreaming(ip)
                        } else {
                            Toast.makeText(this, "Enter IP Address", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        requestPermissions()
                    }
                }
            }
        }

        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioService?.setVolume(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnMute.setOnCheckedChangeListener { _, isChecked ->
            // Checked = Mute Active
            audioService?.setMute(isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun updateUiState(streaming: Boolean) {
        if (streaming) {
            btnConnect.text = "Disconnect"
            tvStatus.text = "Status: Streaming"
            etIpAddress.isEnabled = false
        } else {
            btnConnect.text = "Connect"
            tvStatus.text = "Status: Disconnected"
            etIpAddress.isEnabled = true
        }
    }

    private fun checkPermissions(): Boolean {
        // Android 14 (SDK 34) might need FOREGROUND_SERVICE_MICROPHONE runtime check? 
        // Actually permissions act normal, but Service declaration handles the type.
        // RECORD_AUDIO is the main runtime permission.
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        // Request POST_NOTIFICATIONS (SDK 33+) if needed? 
        // For foreground service notification, it's allowed by default but good practice to ask if we want to show other notifs. 
        // Minimal set for now.
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        
        // SDK 33+ notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 permissions.add(Manifest.permission.POST_NOTIFICATIONS)
             }
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            100
        )
    }
}
