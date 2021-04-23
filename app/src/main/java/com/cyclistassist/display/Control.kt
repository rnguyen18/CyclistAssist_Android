package com.cyclistassist.display

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.pow

const val SPEED_FACTOR = 178.5
const val BUTTON_OFF = "#555555"
const val BUTTON_ON = "#eaf734"

class Control : AppCompatActivity(), BluetoothControllerInterface {

    lateinit var bluetoothController : BluetoothController
    lateinit var bluetoothDevice : BluetoothDevice
    val speedometerTextView: TextView by lazy { this.findViewById(R.id.speedometer) as TextView}
    val radarImage1: ImageView by lazy { this.findViewById(R.id.radar_1) as ImageView }
    val radarImage2: ImageView by lazy { this.findViewById(R.id.radar_2) as ImageView }
    val RadarImages: ArrayList<Drawable?> = ArrayList()

    var lightType: LightType = LightType.NONE
    var headlight = false
    var wheelSize: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
        bluetoothController = BluetoothController(this, bluetoothAdapter, this)

        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_0))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_1))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_2))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_3))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_4))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_5))
        RadarImages.add(ContextCompat.getDrawable(this, R.drawable.radar_6))

        //if(fileExists(this, "wheelSize")) {
            this.openFileInput("wheelSize").bufferedReader().useLines { lines ->
                val value = lines.fold("") { s, t ->
                    s.plus(t)
                }

                wheelSize = value.toDouble()
            }
        //}

        speedometerTextView.text = "00"

        if (intent.hasExtra("bluetoothDevice")) {
            bluetoothDevice = intent.getParcelableExtra("bluetoothDevice") as BluetoothDevice
            bluetoothController.connectDevice(bluetoothDevice)
        }

        val leftButton : Button = findViewById(R.id.btn_left)
        val rightButton : Button = findViewById(R.id.btn_right)
        val hazardButton : Button = findViewById(R.id.btn_hazard)
        val headlightButton : Button = findViewById(R.id.btn_headlights)

        leftButton.setOnClickListener {
            if (lightType != LightType.LEFT) {
                lightType = LightType.LEFT
                bluetoothController.writeCustomCharacteristic("L")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_yellow), PorterDuff.Mode.MULTIPLY)
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("l")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
            }
        }
        rightButton.setOnClickListener {
            if (lightType != LightType.RIGHT) {
                lightType = LightType.RIGHT
                bluetoothController.writeCustomCharacteristic("R")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_yellow), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("r")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
            }
        }

        hazardButton.setOnClickListener {
            if (lightType != LightType.HAZARD) {
                lightType = LightType.HAZARD
                bluetoothController.writeCustomCharacteristic("B")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_yellow), PorterDuff.Mode.MULTIPLY);
            } else {
                lightType = LightType.NONE
                bluetoothController.writeCustomCharacteristic("b")
                leftButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                rightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                hazardButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
            }
        }

        headlightButton.setOnClickListener {
            headlight = if (!headlight) {
                bluetoothController.writeCustomCharacteristic("H")
                headlightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_yellow), PorterDuff.Mode.MULTIPLY);
                true
            } else {
                bluetoothController.writeCustomCharacteristic("h")
                headlightButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.app_gray), PorterDuff.Mode.MULTIPLY);
                false
            }
        }
    }

    override fun DeviceConnected() {
        this.findViewById<RelativeLayout>(R.id.LoadingWindow).visibility = View.INVISIBLE
    }

    override fun ReadSpeedometer(speed: String) {
        Handler(Looper.getMainLooper()).post(Runnable {
            var speedInt: Int = 0
            if (speed.substring(1).toInt() != 0) {
                speedInt = Math.round((SPEED_FACTOR / speed.substring(1).toDouble()) * wheelSize).toInt()
            }
            if (speedInt < 5) {
                speedInt = 0
            }
            val speedometerText = speedInt.toString()

            speedometerTextView.text = speedometerText
        })
    }

    override fun ReadRadar(radar: String) {
        Handler(Looper.getMainLooper()).post(Runnable {
            var radarInt: Double = radar.substring(1).toDouble()

            if (radarInt != 0.0) {
                for (i in 6 downTo 0 step 1) {
                    val compareNum: Double = 2.0.pow(i.toDouble())
                    if (radarInt / compareNum >= 1) {
                        radarImage1.visibility = View.VISIBLE
                        radarImage1.setImageDrawable(RadarImages[i])
                        radarInt -= compareNum
                        break
                    }
                }
            } else {
                radarImage1.visibility = View.GONE
            }

            if (radarInt != 0.0) {
                for (i in 5 downTo 0 step 1) {
                    val compareNum: Double = 2.0.pow(i.toDouble())
                    if (radarInt / compareNum >= 1) {
                        radarImage2.visibility
                        radarImage2.setImageDrawable(RadarImages[i])
                        break
                    }
                }
            } else {
                radarImage2.visibility = View.GONE
            }
        })
    }

    private fun fileExists(context: Context, filename: String?): Boolean {
        val file = context.getFileStreamPath(filename)
        return !(file == null || !file.exists())
    }
}

 enum class LightType {RIGHT, LEFT, HAZARD, NONE}
