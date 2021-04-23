package com.cyclistassist.display

import android.bluetooth.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import java.util.*
import java.util.regex.Pattern

private const val TAG = "BluetoothLeController"
private const val ServiceUUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
private const val CharacteristicUUID = "0000FFE1-0000-1000-8000-00805F9B34FB"
private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

class BluetoothController(
    private val context: Context,
    bluetoothAdapter: BluetoothAdapter,
    bluetoothControllerInterface: BluetoothControllerInterface
) {
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
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
                bluetoothGatt = bluetoothDevice!!.connectGatt(context, false, this)
            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val deviceName = gatt!!.device.name
            super.onServicesDiscovered(gatt, status)
            printGattTable()
            writeCustomCharacteristic("C")
            val mCustomService: BluetoothGattService? = bluetoothGatt?.getService(
                UUID.fromString(
                    ServiceUUID
                )
            )
            if (mCustomService == null) {
                Log.w(TAG, "Custom BLE Service not found")
                return
            }

            val characteristic = mCustomService.getCharacteristic(UUID.fromString(CharacteristicUUID))

            // setNotify(characteristic)

            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(UUID.fromString(
                CCC_DESCRIPTOR_UUID
            ))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)

            bluetoothControllerInterface.DeviceConnected()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val value = characteristic.value

            val stringArr = Pattern.compile(" ").split(String(value))
            for (str in stringArr) {
                Log.i("BLE", str)
                if (str.elementAt(0) == 'S')
                    bluetoothControllerInterface.ReadSpeedometer(str)
                else if (str.elementAt(0) == 'R')
                    bluetoothControllerInterface.ReadRadar(str)
            }
        }
    }


    fun setNotify(characteristic: BluetoothGattCharacteristic) {
        // Get the CCC Descriptor for the characteristic
        val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(
            UUID.fromString(
                CCC_DESCRIPTOR_UUID
            )
        );

        // Check if characteristic has NOTIFY or INDICATE properties and set the correct byte value to be written
        val value: ByteArray = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

        // Then write to descriptor
        descriptor.value = value
        val result: Boolean? = bluetoothGatt?.writeDescriptor(descriptor)
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
        val mReadCharacteristic = mCustomService.getCharacteristic(
            UUID.fromString(
                CharacteristicUUID
            )
        )
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
        val mCustomService: BluetoothGattService? = bluetoothGatt?.getService(
            UUID.fromString(
                ServiceUUID
            )
        )

        if (mCustomService == null) {
            Log.w(TAG, "Custom BLE Service not found")
            return
        }

        /*get the read characteristic from the service*/
        val mWriteCharacteristic = mCustomService.getCharacteristic(
            UUID.fromString(
                CharacteristicUUID
            )
        )
        //mWriteCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        mWriteCharacteristic.setValue(value)
        if (!bluetoothGatt!!.writeCharacteristic(mWriteCharacteristic)) {
            Log.w(TAG, "Failed to write characteristic")
        }
    }

    private fun printGattTable() {
        if (bluetoothGatt!!.services.isEmpty()) {
            Log.i(
                "printGattTable",
                "No service and characteristic available, call discoverServices() first?"
            )
            return
        }
        bluetoothGatt!!.services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i(
                "printGattTable",
                "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }
}