// File: BluetoothControllerManager.kt
package com.dss.emulator.bluetooth.central

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.util.Log
import com.dss.emulator.bluetooth.Constants

class BluetoothControllerManager(
    val bluetoothManager: BluetoothManager,
    val onDeviceConnected: (BluetoothDevice) -> Unit
) {
    val adapter: BluetoothAdapter = bluetoothManager.adapter
    var bluetoothGatt: BluetoothGatt? = null

    val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE Gatt", "Connected to GATT server.")
                gatt.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE Gatt", "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services
                for (service in services) {
                    if (service.uuid.equals(Constants.UDB_SERVICE_UUID)) {
                        onDeviceConnected(gatt.device)
                    }
                }
                Log.d("BLE Gatt", "Services discovered: $services")
            } else {
                Log.w("BLE Gatt", "onServicesDiscovered received: $status")
            }
        }
    }
}