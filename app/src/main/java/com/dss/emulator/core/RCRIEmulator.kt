package com.dss.emulator.core

import android.util.Log
import com.dss.emulator.bluetooth.peripheral.BLEPeripheralController
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.dsscommand.StandardResponse
import com.dss.emulator.register.registerMap

class RCRIEmulator : IEmulator {
    companion object {
        private lateinit var instance: RCRIEmulator

        @Synchronized
        fun getInstance(): IEmulator {
            if (!::instance.isInitialized) {
                instance = RCRIEmulator()
            }
            return instance
        }

        const val PASSWORD = "1776"
    }

    private val blePeripheralController: BLEPeripheralController = TODO()

    private constructor() {}

    override fun sendData(data: ByteArray) {
         blePeripheralController.sendData(data)
    }

    override fun parseDollarCommand(command: DSSCommand) {
        this.handleCommand(command)
    }

    override fun parseBinaryCommand(data: ByteArray) {
        TODO("Not yet implemented")
    }

    private fun parseOKResponse(command: DSSCommand) {
        require(command.command == StandardResponse.OK.toString()) { "Invalid command: Expected OK (OK) command" }
        require(command.data.isEmpty()) { "Invalid data size: Expected no data entries" }

        Log.d("RCRIEmulator", "parseOKResponse")
    }

    private fun parseNOResponse(command: DSSCommand) {
        require(command.command == StandardResponse.NO.toString()) { "Invalid command: Expected NO (NO) command" }
        require(command.data.size == 0) { "Invalid data size: Expected no data entries" }

        Log.d("RCRIEmulator", "parseNOResponse")
    }

    private fun parseIDResponse(command: DSSCommand) {
        require(command.command == StandardResponse.ID.toString()) { "Invalid command: Expected ID (ID) command" }
        require(command.data.size == 1) { "Invalid data size: Expected exactly one data entry" }
        val serialNumber = command.data[0]

        Log.d("RCRIEmulator", "parseIDResponse: $serialNumber")
    }

    private fun parseRTResponse(command: DSSCommand) {
        require(command.command == StandardResponse.RT.toString()) { "Invalid command: Expected RT (RT) command" }
        require(command.data.size == 2) { "Invalid data size: Expected no data entries" }

        val registerName = command.data[0]
        val registerValue = command.data[1]

        Log.d("RCRIEmulator", "parseRTResponse: $registerName $registerValue")

        registerMap[registerName]?.setValueString(registerValue)
    }

    private fun handleCommand(command: DSSCommand) {
        when (command.command) {
            StandardResponse.OK.toString() -> parseOKResponse(command)
            StandardResponse.NO.toString() -> parseNOResponse(command)
            StandardResponse.ID.toString() -> parseIDResponse(command)
            StandardResponse.RT.toString() -> parseRTResponse(command)
            else -> throw IllegalArgumentException("Unknown command: ${command.command}")
        }
    }
}
