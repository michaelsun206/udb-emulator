package com.dss.emulator.bluetooth.peripheral

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log

class BLEPeripheralController(
    context: Context,
    private val onDeviceConnected: (BluetoothDevice?) -> Unit,
    private val onCommandReceived: (String) -> Unit
) {
    private val advertiser: BLEAdvertiser by lazy { BLEAdvertiser(bluetoothManager.adapter.bluetoothLeAdvertiser) }
    private var bluetoothManager: BluetoothManager =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)!!
    private val isAdvertising = false

    @SuppressLint("MissingPermission")
    private var gattServerManager: GattServerManager =
        GattServerManager(context, bluetoothManager, onDeviceConnected = { device ->
            Log.d("BLEPeripheralController", "Device connected: ${device?.name}")
            onDeviceConnected(device)
        }, onCommandReceived = { command ->
            Log.d("BLEPeripheralController", "Command received: $command")
        })

    fun startAdvertising() {
        gattServerManager.startGattServer()
        if (!isAdvertising) advertiser.startAdvertising()
        else Log.d("BLEPeripheralController", "Advertising is already running")
    }

    fun stopAdvertising() {
        if (isAdvertising) advertiser.stopAdvertising()
        else Log.d("BLEPeripheralController", "Advertising is not running")
    }

    fun sendCommand(command: String) {
        gattServerManager.sendCommand(command)
    }
}
