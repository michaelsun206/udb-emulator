package com.dss.emulator.core

import android.content.Context
import android.util.Log
import com.dss.emulator.bluetooth.central.BLECentralController
import com.dss.emulator.bluetooth.peripheral.BLEPeripheralController
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.dsscommand.StandardRequest
import com.dss.emulator.dsscommand.StandardResponse
import com.dss.emulator.register.registerList
import com.dss.emulator.register.registerMap

class RCRIEmulator : IEmulator {

    companion object {
        const val PASSWORD = "1776"
    }

    private val bleCentralController: BLECentralController;

    constructor(context: Context, bleCentralController: BLECentralController) : super(context) {
        this.bleCentralController = bleCentralController
        this.setSource("RC-RI")
        this.setDestination("UDB")
    }

    override fun sendData(data: ByteArray) {
         this.bleCentralController.sendData(data)
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


    // Function to parse the Register Map change report (RM) command
    private fun parseRegisterMapChangeReportCommand(command: DSSCommand) {
        require(command.command == StandardRequest.RM.toString()) { "Invalid command: Expected Register Map Change Report (RM) command" }
        require(command.data.size == 1) { "Invalid data size: Expected one data entry here" }

        Log.d("UDBEmulator", "parseRegisterMapChangeReportCommand")

        val registerMapBit = command.data[0].toLong()

        for (register in registerList) {
            if ((1L shl register.regMapBit) and registerMapBit != 0L) {
                Log.d("UDBEmulator", "parseRegisterMapChangeReportCommand: ${register.name}")

                this.sendCommand(
                    DSSCommand.createGTCommand(
                        this.getDestination(), this.getSource(), register.name
                    )
                )
            }
        }
    }


    private fun handleCommand(command: DSSCommand) {
        when (command.command) {
            StandardResponse.OK.toString() -> parseOKResponse(command)
            StandardResponse.NO.toString() -> parseNOResponse(command)
            StandardResponse.ID.toString() -> parseIDResponse(command)
            StandardResponse.RT.toString() -> parseRTResponse(command)
            StandardRequest.RM.toString() -> parseRegisterMapChangeReportCommand(command)
            else -> throw IllegalArgumentException("Unknown command: ${command.command}")
        }
    }
}
