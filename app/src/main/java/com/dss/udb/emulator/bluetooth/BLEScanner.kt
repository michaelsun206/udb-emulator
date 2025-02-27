// File: BLEScanner.kt
package com.dss.udb.emulator.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.dss.udb.emulator.Constants
import com.dss.udb.emulator.UDBDevice

class BLEScanner(
    private val controllerManager: BluetoothControllerManager,
    private val onDeviceFound: (UDBDevice) -> Unit
) {

    private val scannedDevices = mutableListOf<UDBDevice>()
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val device = UDBDevice(
                    address = it.device.address,
                    name = it.device.name ?: "Unknown",
                    rssi = it.rssi
                )
                if (!scannedDevices.any { d -> d.address == device.address }) {
                    scannedDevices.add(device)
                    Log.d("BLE Scanner", "Device found: ${device.address} ${device.name}")
                    onDeviceFound(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLE Scanner", "Scan failed with error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        controllerManager.adapter.bluetoothLeScanner.startScan(
            null,
            Constants.SCAN_SETTINGS,
            scanCallback
        )
        Log.d("BLE Scanner", "Started scanning")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        controllerManager.adapter.bluetoothLeScanner.stopScan(scanCallback)
        Log.d("BLE Scanner", "Stopped scanning")
    }
}
