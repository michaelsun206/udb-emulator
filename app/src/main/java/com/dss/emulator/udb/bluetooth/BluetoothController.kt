package com.dss.emulator.udb.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.dss.emulator.udb.PermissionsManager
import com.dss.emulator.udb.UDBDevice

class BluetoothController(
    private val context: Context,
    private val onDeviceFound: (UDBDevice) -> Unit,
    private val onPermissionsDenied: () -> Unit
) {

    private val permissionsManager = PermissionsManager(context) { granted ->
        if (granted) {
            initializeBluetoothComponents()
        } else {
            onPermissionsDenied()
        }
    }

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothControllerManager: BluetoothControllerManager

    private lateinit var scanner: BLEScanner
    private lateinit var advertiser: BLEAdvertiser
    private lateinit var gattServerManager: GattServerManager

    fun start() {
        permissionsManager.checkAndRequestPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun initializeBluetoothComponents() {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothControllerManager = BluetoothControllerManager(bluetoothManager)

        scanner = BLEScanner(bluetoothControllerManager, onDeviceFound)
        advertiser = BLEAdvertiser(bluetoothControllerManager)
        gattServerManager = GattServerManager(context, bluetoothControllerManager)

        gattServerManager.startGattServer()
        advertiser.startAdvertising()
        scanner.startScanning()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: UDBDevice) {
        // Implement connection logic or delegate to a separate GattClientManager if needed
        // For simplicity, it's kept here
        val bluetoothDevice = bluetoothControllerManager.adapter.getRemoteDevice(device.address)
        bluetoothControllerManager.bluetoothGatt = bluetoothDevice.connectGatt(
            context, false, bluetoothControllerManager.gattCallback
        )
        Log.d("BLE Gatt", "Connecting to device: ${device.address}")
    }

    fun stop() {
        gattServerManager.stopGattServer()
        advertiser.stopAdvertising()
        scanner.stopScanning()
    }


    fun sendNotification(value: String) {
        gattServerManager.notifyCharacteristic1(value)
    }
}