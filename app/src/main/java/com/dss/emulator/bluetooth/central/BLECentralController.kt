package com.dss.emulator.bluetooth.central

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.dss.emulator.UDBDevice
import com.dss.emulator.bluetooth.Constants

class BLECentralController(
    private val context: Context,
    private val onDeviceFound: (UDBDevice) -> Unit,
    private val onDeviceConnected: (BluetoothDevice) -> Unit
) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val gattCallback = object : BluetoothGattCallback() {
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services
                for (service in services) {
                    Log.d("BLE Gatt", "Services discovered: ${service.uuid}")
                    if (service.uuid.equals(Constants.UDB_SERVICE_UUID)) {
                        onDeviceConnected(gatt.device)
                    }
                }
            } else {
                Log.w("BLE Gatt", "onServicesDiscovered received: $status")
            }
        }
    }

    private val scanner: BLEScanner by lazy {
        BLEScanner(
            bluetoothManager = bluetoothManager!!, onDeviceFound
        )
    }

    fun startScanning() {
        scanner.startScanning()
    }

    fun stopScanning() {
        scanner.stopScanning()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: UDBDevice) {
        val bluetoothDevice = bluetoothManager?.adapter?.getRemoteDevice(device.address)
        bluetoothGatt = bluetoothDevice?.connectGatt(
            context, false, gattCallback
        )

        Log.d("BLECentralController", "Connecting to device: ${device.address}")
    }
}
