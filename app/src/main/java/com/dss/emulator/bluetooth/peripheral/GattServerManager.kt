// File: GattServerManager.kt
package com.dss.emulator.bluetooth.peripheral

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.dss.emulator.bluetooth.Constants

class GattServerManager(
    private val context: Context, private val bluetoothManager: BluetoothManager
) {

    private var connectedDevice: BluetoothDevice? = null;
    private var characteristic1: BluetoothGattCharacteristic? = null;
    private var characteristic2: BluetoothGattCharacteristic? = null;
    private var bluetoothGattServer: BluetoothGattServer? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d("GattServer", "Device $device connection state changed: $newState")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
            } else {
                connectedDevice = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let {
                when (it.uuid) {
                    Constants.DATA_READ_CHARACTERISTIC_UUID -> {
                        val value = Constants.CHARACTERISTIC_1_VALUE.toByteArray()
                        bluetoothGattServer?.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value
                        )
                    }

                    Constants.COMMAND_WRITE_CHARACTERISTIC_UUID -> {
                        val value = Constants.CHARACTERISTIC_2_VALUE.toByteArray()
                        bluetoothGattServer?.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value
                        )
                    }

                    else -> {}
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            characteristic?.let {
                if (it.uuid == Constants.COMMAND_WRITE_CHARACTERISTIC_UUID) {
                    val receivedData = String(value ?: byteArrayOf())
                    Log.d("GattServer", "Received data on Characteristic 2: $receivedData")
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            descriptor?.let {
                if (it.uuid == Constants.CHARACTERISTIC_USER_DESCRIPTION_UUID) {
                    val value = when (it.characteristic.uuid) {
                        Constants.DATA_READ_CHARACTERISTIC_UUID -> Constants.DATA_READ_DESCRIPTOR.toByteArray()
                        Constants.COMMAND_WRITE_CHARACTERISTIC_UUID -> Constants.COMMAND_WRITE_DESCRIPTOR.toByteArray()
                        else -> byteArrayOf()
                    }
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            descriptor?.let {
                if (it.uuid == Constants.CHARACTERISTIC_USER_DESCRIPTION_UUID) {
                    val receivedValue = String(value ?: byteArrayOf())
                    Log.d("GattServer", "Received value for descriptor: $receivedValue")
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startGattServer() {
        try {
            bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            bluetoothGattServer?.addService(buildGattService())
            Log.d("GattServer", "GATT Server started")
        } catch (e: Exception) {
            Log.e("GattServer", "Failed to start GATT server: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopGattServer() {
        bluetoothGattServer?.close()
        Log.d("GattServer", "GATT Server stopped")
    }

    private fun buildGattService(): BluetoothGattService {
        val service = BluetoothGattService(
            Constants.UDB_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Characteristic for data read
        characteristic1 = BluetoothGattCharacteristic(
            Constants.DATA_READ_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        //add descriptor
        val descriptor = BluetoothGattDescriptor(
            Constants.CHARACTERISTIC_USER_DESCRIPTION_UUID, BluetoothGattDescriptor.PERMISSION_READ
        )
        characteristic1!!.addDescriptor(descriptor);
        service.addCharacteristic(characteristic1)

        // Characteristic for command write
        characteristic2 = BluetoothGattCharacteristic(
            Constants.COMMAND_WRITE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        characteristic2!!.addDescriptor(descriptor);
        service.addCharacteristic(characteristic2)

        return service
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun notifyCharacteristic1(value: String) {
        bluetoothGattServer?.notifyCharacteristicChanged(
            connectedDevice!!, characteristic1!!, false, value.toByteArray()
        )
    }
}