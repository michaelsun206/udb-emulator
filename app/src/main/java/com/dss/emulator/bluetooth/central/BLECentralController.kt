package com.dss.emulator.bluetooth.central

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.dss.emulator.UDBDevice

class BLECentralController(
    private val context: Context,
    private val onDeviceFound: (UDBDevice) -> Unit,
    private val onDeviceConnected: () -> Unit
) {
    private val tag = javaClass.simpleName

    private lateinit var bluetoothControllerManager: BluetoothControllerManager
    private val scanner: BLEScanner by lazy {
        BLEScanner(
            bluetoothControllerManager,
            onDeviceFound
        )
    }

    init {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager

        bluetoothManager?.let {
            bluetoothControllerManager = BluetoothControllerManager(it, onDeviceConnected = {
                onDeviceConnected()
            })
        } ?: throw IllegalStateException("BluetoothManager not available")
    }

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
            context, true, bluetoothControllerManager.gattCallback
        )
        Log.d(tag, "Connecting to device: ${device.address}")
    }
}
