package com.dss.emulator.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.dss.emulator.udb.R
import com.dss.emulator.bluetooth.BLEPermissionsManager
import com.dss.emulator.bluetooth.central.BLECentralController

class RcRiEmulatorActivity : ComponentActivity() {

    private lateinit var permissionsManager: BLEPermissionsManager
    private lateinit var devicesDialog: FindDevicesDialog

    private var bleCentralController: BLECentralController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rc_ri_emulator)

        permissionsManager = BLEPermissionsManager(this) { granted ->
            if (!granted) {
                Log.e("Permissions", "Bluetooth permissions denied")

                // Alert and Exit
                AlertDialog.Builder(this)
                    .setTitle("Bluetooth Permissions Denied")
                    .setMessage("Please grant Bluetooth permissions to use this app.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }


        bleCentralController = BLECentralController(
            context = this,
            onDeviceFound = { device ->
                runOnUiThread {
                    devicesDialog.addDevice(device)
                }
            },
            onDeviceConnected = {
                runOnUiThread {
                    devicesDialog.stopScanning()
                    devicesDialog.dismiss()
                }
            }
        )

        devicesDialog = FindDevicesDialog(
            bleCentralController = bleCentralController!!,
            context = this,
            onDeviceSelected = { device ->
                Log.d("DeviceSelected", "Selected device: ${device.name} ${device.address}")
                bleCentralController?.connectToDevice(device)
            }
        )

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
        devicesDialog.startScanning()
        devicesDialog.setTitle("Finding UDB Devices...")
        devicesDialog.show()
    }
}
