package com.dss.emulator.core

import android.content.Context
import android.util.Log
import com.dss.emulator.bluetooth.peripheral.BLEPeripheralController
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.dsscommand.StandardRequest
import com.dss.emulator.register.Direction
import com.dss.emulator.register.Registers
import com.dss.emulator.register.registerList
import com.dss.emulator.register.registerMap

class UDBEmulator : IEmulator {

    companion object {
        const val PASSWORD = "1776"
    }


    private val blePeripheralController: BLEPeripheralController;

    constructor(context: Context, blePeripheralController: BLEPeripheralController) : super(context) {
        this.blePeripheralController = blePeripheralController
        this.setSource("UDB")
        this.setDestination("RC-RI")
    }

    override fun sendData(data: ByteArray) {
        this.blePeripheralController.sendData(data)
    }

    override fun parseDollarCommand(command: DSSCommand) {
        this.handleCommand(command)
    }

    override fun parseBinaryCommand(data: ByteArray) {
        TODO("Not yet implemented")
    }


    // Function to parse the GET (GT) command
    private fun parseRegisterGetCommand(command: DSSCommand) {
        require(command.command == StandardRequest.GT.toString()) { "Invalid command: Expected GET (GT) command" }
        require(command.data.size == 1) { "Invalid data size: Expected exactly one data entry" }

        val registerName = command.data[0]
        val register = registerMap[registerName]
            ?: throw IllegalArgumentException("Invalid register: '$registerName' not found in register map")
        val registerValue = register.getValue().toString()

        Log.d("UDBEmulator", "parseRegisterGetCommand: $registerName $registerValue")

        this.sendCommand(
            DSSCommand.createRTResponse(
                this.getDestination(), this.getSource(), register.name, registerValue
            )
        )
    }

    // Function to parse the SET (ST) command
    private fun parseRegisterSetCommand(command: DSSCommand) {
        require(command.command == StandardRequest.ST.toString()) { "Invalid command: Expected SET (ST) command" }
        require(command.data.size == 2) { "Invalid data size: Expected exactly two data entries" }
        val registerName = command.data[0]
        val registerValue = command.data[1]
        val register = registerMap[registerName]
            ?: throw IllegalArgumentException("Invalid register: '$registerName' not found in register map")
        register.setValueString(registerValue)

        Log.d("UDBEmulator", "parseRegisterSetCommand: $registerName $registerValue")

        this.sendCommand(
            if (register.direction == Direction.GUI_TO_UDB || register.direction == Direction.BOTH) {
                DSSCommand.createOKResponse(this.getDestination(), this.getSource())
            } else {
                DSSCommand.createNOResponse(this.getDestination(), this.getSource())
            }
        )
    }

    // Function to parse the GET ID (GI) command
    private fun parseRegisterGetIDCommand(command: DSSCommand) {
        require(command.command == StandardRequest.GI.toString()) { "Invalid command: Expected GET ID (GI) command" }
        require(command.data.isEmpty()) { "Invalid data size: Expected no data entries" }
        val serialNumber = Registers.SN.getValue().toString()

        Log.d("UDBEmulator", "parseRegisterGetIDCommand: $serialNumber")

        this.sendCommand(
            DSSCommand.createIDResponse(
                this.getDestination(), this.getSource(), serialNumber
            )
        )
    }

    // Function to parse the SET ID (SI) command
    private fun parseRegisterSetIDCommand(command: DSSCommand) {
        require(command.command == StandardRequest.SI.toString()) { "Invalid command: Expected SET ID (SI) command" }
        require(command.data.size == 2) { "Invalid data size: Expected exactly two data entries" }
        val password = command.data[0]
        val serialNumber = command.data[1]

        Log.d("UDBEmulator", "parseRegisterSetIDCommand: $password $serialNumber")

        this.sendCommand(
            if (password == PASSWORD) {
                Registers.SN.setValueString(serialNumber)
                DSSCommand.createOKResponse(this.getDestination(), this.getSource())
            } else {
                DSSCommand.createNOResponse(this.getDestination(), this.getSource())
            }
        )
    }

    // Function to parse the SET Protected Register (SP) command
    private fun parseSetProtectedRegisterCommand(command: DSSCommand) {
        require(command.command == StandardRequest.SP.toString()) { "Invalid command: Expected SET Protected Register (SP) command" }
        require(command.data.size == 3) { "Invalid data size: Expected exactly three data entries" }
        val password = command.data[0]
        val registerName = command.data[1]
        val registerValue = command.data[2]

        Log.d(
            "UDBEmulator",
            "parseSetProtectedRegisterCommand: $password $registerName $registerValue"
        )

        if (password == Companion.PASSWORD) {
            val register = registerMap[registerName]
                ?: throw IllegalArgumentException("Invalid register: '$registerName' not found in register map")
            register.setValueString(registerValue)

            this.sendCommand(
                if (register.direction == Direction.GUI_TO_UDB || register.direction == Direction.BOTH) {
                    DSSCommand.createOKResponse(this.getDestination(), this.getSource())
                } else {
                    DSSCommand.createNOResponse(this.getDestination(), this.getSource())
                }
            )
        }
        this.sendCommand(DSSCommand.createNOResponse(this.getDestination(), this.getSource()))
    }

    // Function to parse the Factory Test (FT) command
    private fun parseFactoryTestCommand(command: DSSCommand) {
        require(command.command == StandardRequest.FT.toString()) { "Invalid command: Expected Factory Test (FT) command" }
        // Implement the factory test initiation logic here
        // For demonstration, we'll return an OK response

        Log.d("UDBEmulator", "parseFactoryTestCommand")
    }

    // Function to parse the Reboot (RB) command
    private fun parseRebootCommand(command: DSSCommand) {
        require(command.command == StandardRequest.RB.toString()) { "Invalid command: Expected Reboot (RB) command" }
        // Implement the reboot logic here
        // For demonstration, we'll return an OK response

        Log.d("UDBEmulator", "parseRebootCommand")
    }

    private fun handleCommand(command: DSSCommand) {
        when (command.command) {
            StandardRequest.GT.toString() -> parseRegisterGetCommand(command)
            StandardRequest.ST.toString() -> parseRegisterSetCommand(command)
            StandardRequest.SP.toString() -> parseSetProtectedRegisterCommand(command)
            StandardRequest.GI.toString() -> parseRegisterGetIDCommand(command)
            StandardRequest.SI.toString() -> parseRegisterSetIDCommand(command)
            StandardRequest.FT.toString() -> parseFactoryTestCommand(command)
            StandardRequest.RB.toString() -> parseRebootCommand(command)
            else -> throw IllegalArgumentException("Unknown command: ${command.command}")
        }
    }
}
