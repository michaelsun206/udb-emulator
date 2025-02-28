package com.dss.emulator.udb

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dss.emulator.udb.bluetooth.BluetoothController
import com.dss.emulator.udb.R

class RcRiEmulatorActivity : ComponentActivity() {

    private lateinit var bluetoothController: BluetoothController
    private lateinit var udbDeviceAdapter: UDBDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rc_ri_emulator)

        initializeButtonActions();
        setupRecyclerView()
        initializeBluetooth()
    }

    private fun initializeButtonActions() {
        findViewById<android.widget.Button>(R.id.sendButton).setOnClickListener {
            bluetoothController.sendNotification("Hello From UDB");
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        udbDeviceAdapter = UDBDeviceAdapter { device ->
            bluetoothController.connectToDevice(device)
        }
        recyclerView.adapter = udbDeviceAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun initializeBluetooth() {
        bluetoothController = BluetoothController(
            context = this,
            onDeviceFound = { device ->
                runOnUiThread {
                    udbDeviceAdapter.addDevice(device)
                }
            },
            onPermissionsDenied = {
                Log.e("Permissions", "Bluetooth permissions denied")
            }
        )
        bluetoothController.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothController.stop()
    }
}
