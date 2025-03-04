package com.dss.emulator.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.dss.emulator.bluetooth.BLEPermissionsManager
import com.dss.emulator.bluetooth.central.BLECentralController
import com.dss.emulator.udb.R

class RcRiEmulatorActivity : ComponentActivity() {

    private lateinit var permissionsManager: BLEPermissionsManager
    private lateinit var devicesDialog: FindDevicesDialog

    private var bleCentralController: BLECentralController? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rc_ri_emulator)

        initializeSendButton()

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
        }, onCommandReceived = { command ->
            runOnUiThread {
                AlertDialog.Builder(this).setTitle("Command Received")
                    .setMessage("Command Received: $command").setPositiveButton("OK") { _, _ ->
                    }.show()
            }
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

    private fun initializeSendButton() {
        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.sendCommandButton).setOnClickListener {
            bleCentralController?.sendCommand("Hello From RC!!!")
        }
    }
}
