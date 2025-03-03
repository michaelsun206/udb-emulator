package com.dss.emulator.bluetooth.peripheral

import android.bluetooth.BluetoothManager
import android.content.Context

class BLEPeripheralController(
    private val context: Context
) {
    private val advertiser: BLEAdvertiser by lazy { BLEAdvertiser(bluetoothManager.adapter.bluetoothLeAdvertiser) }
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var gattServerManager: GattServerManager

    fun initialize() {
        bluetoothManager =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)!!

        gattServerManager =
            GattServerManager(
                context, bluetoothManager
            )
    }

    fun startAdvertising() {
        gattServerManager.startGattServer()
        advertiser.startAdvertising()
    }

    fun stopAdvertising() {
        gattServerManager.stopGattServer()
        advertiser.stopAdvertising()
    }

    fun sendNotification(value: String) {
        gattServerManager.notifyCharacteristic1(value)
    }
}
