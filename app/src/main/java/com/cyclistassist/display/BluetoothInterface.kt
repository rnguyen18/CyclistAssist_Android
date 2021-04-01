package com.cyclistassist.display

import android.bluetooth.BluetoothDevice

interface BluetoothInterface {
    fun connectDevice(device : BluetoothDevice)
    fun disconnectDevice()
    fun checkConnection() : Boolean
    fun checkDevice(device: BluetoothDevice) : Boolean
    fun foundDevice(device: BluetoothDevice)
    fun setStatus(message: String)
    fun scanMessage(message: String)
    fun NotifyChange()
}