package com.cyclistassist.display

interface BluetoothControllerInterface {
    fun DeviceConnected()
    fun ReadSpeedometer(speed: String)
    fun ReadRadar(radar: String)
}