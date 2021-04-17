package com.cyclistassist.display

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import java.io.Serializable
import java.util.*


private const val TAG = "BluetoothLeController"
private const val ServiceUUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
private const val CharacteristicUUID = "0000FFE1-0000-1000-8000-00805F9B34FB"

class BluetoothController(private val context: Context, bluetoothAdapter: BluetoothAdapter, bluetoothControllerInterface: BluetoothControllerInterface) {
    private var scanning = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothService : BluetoothLeService? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private val gattServiceIntent by lazy { Intent(context, BluetoothLeService::class.java) }

    fun connectDevice(device: BluetoothDevice) {
        bluetoothDevice = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceName = gatt.device.name

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            gatt.discoverServices()
                        }, 1000)
                    }
                    BluetoothGatt.STATE_DISCONNECTED -> {
                        gatt.close()
                    }
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
                bluetoothGatt = bluetoothDevice!!.connectGatt(context, false, this)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val deviceName = gatt!!.device.name
            super.onServicesDiscovered(gatt, status)
            printGattTable()
            writeCustomCharacteristic("C")
            bluetoothControllerInterface.DeviceConnected()
        }
    }

    fun disconnectDevice() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    // finish()
                } else {
                    // perform device connection
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    init {context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)}

    fun readCustomCharacteristic() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        /*check if the service is available on the device*/
        val mCustomService: BluetoothGattService =
            bluetoothGatt!!.getService(UUID.fromString(ServiceUUID))

        /*get the read characteristic from the service*/
        val mReadCharacteristic =
            mCustomService.getCharacteristic(UUID.fromString(CharacteristicUUID))
        if (!bluetoothGatt!!.readCharacteristic(mReadCharacteristic)) {
            Log.w(TAG, "Failed to read characteristic")
        }
    }

    fun writeCustomCharacteristic(value: String) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        /*check if the service is available on the device*/
        val mCustomService: BluetoothGattService? = bluetoothGatt?.getService(UUID.fromString(ServiceUUID))
        if (mCustomService == null) {
            Log.w(TAG, "Custom BLE Service not found")
            return
        }

        /*get the read characteristic from the service*/
        val mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(CharacteristicUUID))
        //mWriteCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        mWriteCharacteristic.setValue(value)
        if (!bluetoothGatt!!.writeCharacteristic(mWriteCharacteristic)) {
            Log.w(TAG, "Failed to write characteristic")
        }
    }

    private fun printGattTable() {
        if (bluetoothGatt!!.services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        bluetoothGatt!!.services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        }
    }
}