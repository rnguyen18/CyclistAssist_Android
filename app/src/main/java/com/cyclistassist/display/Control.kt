package com.cyclistassist.display

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Control : AppCompatActivity() {

    lateinit var bluetoothController : BluetoothController
    lateinit var bluetoothDevice : BluetoothDevice
    var lightType: LightType = LightType.NONE
    var headlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
        bluetoothController = BluetoothController(this, bluetoothAdapter)

        if (intent.hasExtra("bluetoothDevice")) {
            bluetoothDevice = intent.getParcelableExtra("bluetoothDevice") as BluetoothDevice
            bluetoothController.connectDevice(bluetoothDevice)
        }

        val leftButton : Button = findViewById(R.id.btn_left)
        leftButton.setOnClickListener {
            if (lightType != LightType.LEFT) {
                lightType = LightType.LEFT
                bluetoothController.writeCustomCharacteristic("L")
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("l")
            }
        }

        val rightButton : Button = findViewById(R.id.btn_right)
        rightButton.setOnClickListener {
            if (lightType != LightType.RIGHT) {
                lightType = LightType.RIGHT
                bluetoothController.writeCustomCharacteristic("R")
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("r")
            }
        }

        val hazardButton : Button = findViewById(R.id.btn_hazard)
        hazardButton.setOnClickListener {
            if (lightType != LightType.HAZARD) {
                lightType = LightType.HAZARD
                bluetoothController.writeCustomCharacteristic("B")
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("b")
            }
        }

        val headlightButton : Button = findViewById(R.id.btn_headlights)
        headlightButton.setOnClickListener {
            headlight = if (!headlight) {
                bluetoothController.writeCustomCharacteristic("H")
                true
            } else {
                bluetoothController.writeCustomCharacteristic("h")
                false
            }
        }
    }
}
 enum class LightType {RIGHT, LEFT, HAZARD, NONE}