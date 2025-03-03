// File: BLEAdvertiser.kt
package com.dss.emulator.bluetooth.peripheral

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.util.Log
import com.dss.emulator.bluetooth.Constants
import com.dss.emulator.bluetooth.central.BluetoothControllerManager

class BLEAdvertiser(
    private val advertiser: BluetoothLeAdvertiser
) {

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