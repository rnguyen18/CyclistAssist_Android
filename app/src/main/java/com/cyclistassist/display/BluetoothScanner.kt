package com.cyclistassist.display

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.Serializable
import java.util.*

private const val TAG = "BluetoothLeScanner"

class BluetoothScanner (private val context: Context, bluetoothAdapter: BluetoothAdapter, private val bluetoothInterface: BluetoothInterface) {
    private var scanning = false
    private val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private val handler = Handler()
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothService : BluetoothLeService? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private val SCAN_PERIOD: Long = 5000 // In milliseconds
    private val gattServiceIntent by lazy { Intent(context, BluetoothLeService::class.java) }

    fun scanLeDevice() {
        bluetoothLeScanner.let { scanner ->
            if (!scanning) {
                handler.postDelayed({
                    stopScanLeDevice(scanner)
                }, SCAN_PERIOD)
                scanning = true
                scanner.startScan(leScanCallback)
                bluetoothInterface.setStatus("Scanning")
                bluetoothInterface.scanMessage("Stop Scan")
            } else {
                stopScanLeDevice(scanner)
            }
        }
    }

    fun stopScanLeDevice() {
        bluetoothLeScanner.let {scanner ->
            if (scanning) {
                scanning = false
                scanner.stopScan(leScanCallback)
                bluetoothInterface.scanMessage("Scan")
                bluetoothInterface.setStatus("Scanning Ended")
            }
        }
    }

    fun stopScanLeDevice(scanner: BluetoothLeScanner) {
        if (scanning) {
            scanning = false
            scanner.stopScan(leScanCallback)
            bluetoothInterface.scanMessage("Scan")
            bluetoothInterface.setStatus("Scanning Ended")
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        if (scanning)
            stopScanLeDevice()

        bluetoothDevice = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.device.name != null) {
                bluetoothInterface.foundDevice(result.device)
            }
        }
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
                        bluetoothInterface.setStatus("Disconnected to $deviceName")
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
            bluetoothInterface.setStatus("Connected to $deviceName")
            Handler(Looper.getMainLooper()).post {
                bluetoothInterface.NotifyChange()
            }
        }
    }

    fun disconnectDevice() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt = null
        bluetoothInterface.NotifyChange()
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

    fun CheckConnection(): Boolean {
        return bluetoothGatt != null
    }

    fun checkDevice(device: BluetoothDevice) : Boolean {
        if (bluetoothGatt == null) {
            return false
        }
        return bluetoothGatt?.device == device
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