package com.example.twinsound

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var status: TextView
    private lateinit var devices: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "TwinSound"
            textSize = 30f
        }

        status = TextView(this).apply {
            text = "Starting TwinSound..."
            textSize = 17f
            setPadding(0, 12, 0, 18)
        }

        devices = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val refresh = Button(this).apply {
            text = "Scan for audio outputs"
            setOnClickListener {
                scan()
            }
        }

        val bluetooth = Button(this).apply {
            text = "Open Bluetooth settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(devices)
        root.addView(refresh)
        root.addView(bluetooth)

        setContentView(root)

        requestBluetoothPermission()
    }

    private fun requestBluetoothPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val connectGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            val scanGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            if (connectGranted && scanGranted) {
                scan()
            } else {
                status.text = "Bluetooth permission is required."

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ),
                    100
                )
            }

        } else {
            scan()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {

            val granted =
                grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }

            if (granted) {
                scan()
            } else {
                status.text =
                    "Bluetooth permission was denied."
            }
        }
    }

    private fun scan() {

        try {

            devices.removeAllViews()

            val outputs =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_OUTPUTS
                )

            val bluetoothDevices = outputs.filter { device ->

                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (
                        device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                    )
                )
            }

            status.text = when {

                bluetoothDevices.size >= 2 ->
                    "✓ ${bluetoothDevices.size} Bluetooth audio outputs detected."

                bluetoothDevices.size == 1 ->
                    "1 Bluetooth audio output detected."

                else ->
                    "No Bluetooth audio outputs detected."
            }

            bluetoothDevices.forEachIndexed { index, device ->

                val name = try {
                    device.productName?.toString()
                        ?: "Unknown speaker"
                } catch (e: SecurityException) {
                    "Bluetooth device"
                }

                val kind = when (device.type) {

                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
                        "Bluetooth A2DP"

                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                        "Bluetooth SCO"

                    AudioDeviceInfo.TYPE_BLE_HEADSET ->
                        "Bluetooth LE headset"

                    AudioDeviceInfo.TYPE_BLE_SPEAKER ->
                        "Bluetooth LE speaker"

                    else ->
                        "Bluetooth audio"
                }

                devices.addView(
                    TextView(this).apply {
                        text = "🔊 ${index + 1}. $name\n   $kind"
                        textSize = 16f
                        setPadding(0, 10, 0, 10)
                    }
                )
            }

        } catch (e: SecurityException) {

            status.text =
                "Bluetooth permission is needed."

        } catch (e: Exception) {

            status.text =
                "TwinSound couldn't scan the audio devices."

        }
    }
}
