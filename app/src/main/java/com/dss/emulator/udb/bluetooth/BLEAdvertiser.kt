// File: BLEAdvertiser.kt
package com.dss.emulator.udb.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.util.Log
import com.dss.emulator.udb.Constants

class BLEAdvertiser(
    private val controllerManager: BluetoothControllerManager
) {

    private val advertiser = controllerManager.adapter.bluetoothLeAdvertiser
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: android.bluetooth.le.AdvertiseSettings) {
            Log.d("BLE Advertiser", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE Advertiser", "Advertising failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        controllerManager.adapter.name = Constants.DEVICE_NAME

        advertiser.startAdvertising(
            Constants.ADVERTISE_SETTINGS,
            Constants.getAdvertiseData(), advertiseCallback
        )
        Log.d("BLE Advertiser", "Started advertising")
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        advertiser.stopAdvertising(advertiseCallback)
        Log.d("BLE Advertiser", "Stopped advertising")
    }
}