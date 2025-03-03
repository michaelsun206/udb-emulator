package com.dss.emulator.bluetooth.central

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.dss.emulator.UDBDevice

class BLECentral(
    private val context: Context,
    private val bluetoothControllerManager: BluetoothControllerManager,
    private val onDeviceFound: (UDBDevice) -> Unit
) {
    private val scanner = BLEScanner(context, bluetoothControllerManager, onDeviceFound)

    fun startScanning() {
        scanner.startScanning()
    }

    fun stopScanning() {
        scanner.stopScanning()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: UDBDevice) {
        val bluetoothDevice = bluetoothControllerManager.adapter.getRemoteDevice(device.address)
        bluetoothControllerManager.bluetoothGatt = bluetoothDevice.connectGatt(
            context, false, bluetoothControllerManager.gattCallback
        )
        Log.d("BLE Gatt", "Connecting to device: ${device.address}")
    }
}
