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

        permissionsManager = BLEPermissionsManager(this) { granted ->
            Log.e("Permissions", "Bluetooth permissions denied")

            if (!granted) {
                Log.e("Permissions", "Bluetooth permissions denied")

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
        })

        devicesDialog = FindDevicesDialog(bleCentralController = bleCentralController!!,
            context = this,
            onDeviceSelected = { device ->
                Log.d("DeviceSelected", "Selected device: ${device.name} ${device.address}")
                bleCentralController?.connectToDevice(device)
            })

        showDeviceListDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
//        bleCentralController?.stopScanning()
    }

//    override fun onResume() {
//        super.onResume()
//        permissionsManager.checkAndRequestPermissions()
//        bleCentralController?.startScanning()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        bleCentralController?.stopScanning()
//    }

    private fun showDeviceListDialog() {
        devicesDialog.show()
        permissionsManager.checkAndRequestPermissions()
        devicesDialog.startScanning()
    }
}
