package com.dss.emulator.bluetooth.central

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dss.emulator.UDBDevice
import com.dss.emulator.bluetooth.Constants


class BLEScanner(
    private val context: Context,
    private val controllerManager: BluetoothControllerManager,
    private val onDeviceFound: (UDBDevice) -> Unit
) {
    private val scannedDevices = mutableListOf<UDBDevice>()
    private var isScanning = false

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLEScanner", "Scan failed with error: $errorCode")
        }

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { deviceInfo ->
                val device = UDBDevice(
                    address = deviceInfo.address,
                    name = deviceInfo.name ?: "Unknown",
                    rssi = result.rssi
                )
                if (scannedDevices.none { it.address == device.address }) {
                    scannedDevices.add(device)
                    Log.d("BLEScanner", "Device found: ${device.address} ${device.name}")
                    onDeviceFound(device)
                }
            }
        }
    }

    //    @SuppressLint("MissingPermission")
    fun startScanning() {
//        if (!hasRequiredPermissions()) {
//            Log.e("BLEScanner", "Missing required BLE permissions or location services disabled.")
//            return
//        }

        if (isScanning) {
            Log.d("BLEScanner", "Scanner already running.")
            return
        }

        val scanner = controllerManager.adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BLEScanner", "bluetoothLeScanner is null - check adapter or Bluetooth state.")
            return
        }

        scannedDevices.clear()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLEScanner", "Missing required BLE permissions or location services disabled.")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        scanner.startScan(null, Constants.SCAN_SETTINGS, scanCallback)
        isScanning = true
        Log.d("BLEScanner", "BLE scan started successfully.")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) {
            Log.d("BLEScanner", "Scanner already stopped.")
            return
        }

        val scanner = controllerManager.adapter.bluetoothLeScanner
        if (scanner != null) {
            scanner.stopScan(scanCallback)
            Log.d("BLEScanner", "BLE scan stopped.")
        } else {
            Log.e("BLEScanner", "Cannot stop scan, bluetoothLeScanner is null.")
        }
        isScanning = false
    }

    // Helper function to check permissions & location state (very important!)
    private fun hasRequiredPermissions(): Boolean {
        val hasScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        val bluetoothEnabled = controllerManager.adapter.isEnabled

        val locationEnabled = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val locationMode = android.provider.Settings.Secure.getInt(
                context.contentResolver,
                android.provider.Settings.Secure.LOCATION_MODE,
                android.provider.Settings.Secure.LOCATION_MODE_OFF
            )
            locationMode != android.provider.Settings.Secure.LOCATION_MODE_OFF
        } else true // BLE location check only for API<31

        if (!bluetoothEnabled) {
            Log.e("BLEScanner", "Bluetooth is disabled on device!")
        }

        if (!locationEnabled) {
            Log.e(
                "BLEScanner",
                "Location (GPS) is disabled on device; required for BLE before Android 12."
            )
        }

        if (!hasScanPermission) {
            Log.e(
                "BLEScanner",
                "Required permission not granted! BLUETOOTH_SCAN (API 31+) or ACCESS_FINE_LOCATION (<API 31)"
            )
        }

        return hasScanPermission && bluetoothEnabled && locationEnabled
    }
}