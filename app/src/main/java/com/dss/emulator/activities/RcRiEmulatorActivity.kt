package com.dss.emulator.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.dss.emulator.bluetooth.BLEPermissionsManager
import com.dss.emulator.bluetooth.central.BLECentralController
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.udb.R

class RcRiEmulatorActivity : ComponentActivity() {

    private lateinit var historyTextView: TextView
    private lateinit var permissionsManager: BLEPermissionsManager
    private lateinit var devicesDialog: FindDevicesDialog

    private var bleCentralController: BLECentralController? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rc_ri_emulator)

        historyTextView = findViewById(R.id.historyTextView)
        historyTextView.text = ""

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.sendCommandButton).setOnClickListener {
            sendCommand("Hello from RC/RI Emulator")
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.commandRBButton).setOnClickListener {
            sendCommand(DSSCommand.createRebootCommand("RC-RI", "UDB").commandText)
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.commandFTButton).setOnClickListener {
            sendCommand(DSSCommand.createRebootCommand("RC-RI", "UDB").commandText)
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.commandGIButton).setOnClickListener {
            sendCommand(DSSCommand.createRebootCommand("RC-RI", "UDB").commandText)
        }


        permissionsManager = BLEPermissionsManager(this) { granted ->
            if (!granted) {
                Log.e("RcRiEmulatorActivity", "Bluetooth permissions denied")

                // Alert and Exit
                AlertDialog.Builder(this).setTitle("Bluetooth Permissions Denied")
                    .setMessage("Please grant Bluetooth permissions to use this app.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }.show()
            }
        }

        bleCentralController = BLECentralController(context = this, onDeviceFound = { device ->
            runOnUiThread {
                devicesDialog.addDevice(device)
            }
        }, onDeviceConnected = {
            runOnUiThread {
                devicesDialog.stopScanning()
                devicesDialog.dismiss()

                AlertDialog.Builder(this).setTitle("Connected to ${it.name}")
                    .setMessage("Connected to ${it.name}").setPositiveButton("OK") { _, _ ->
                    }.show()
            }
        }, onCommandReceived = {
            onCommandReceived(it)
        })

        devicesDialog = FindDevicesDialog(bleCentralController = bleCentralController!!,
            context = this,
            onDeviceSelected = { device ->
                Log.d("DeviceSelected", "Selected device: ${device.name} ${device.address}")
                bleCentralController?.connectToDevice(device)
            })

        showDeviceListDialog()
    }

    private fun showDeviceListDialog() {
        devicesDialog.show()
        permissionsManager.checkAndRequestPermissions()
        devicesDialog.startScanning()
    }

    private fun sendCommand(command: String) {
        historyTextView.text = ">> $command\n${historyTextView.text}"
        bleCentralController?.sendCommand(command)
    }

    private fun onCommandReceived(command: String) {
        historyTextView.text = "<< $command\n${historyTextView.text}"
    }
}
