package com.dss.emulator.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.dss.emulator.bluetooth.BLEPermissionsManager
import com.dss.emulator.bluetooth.central.BLECentralController
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.register.Register
import com.dss.emulator.register.registerList
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
            sendCommand(DSSCommand.createRBCommand("RC-RI", "UDB").commandText)
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.commandFTButton).setOnClickListener {
            sendCommand(DSSCommand.createFTCommand("RC-RI", "UDB").commandText)
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.commandGIButton).setOnClickListener {
            sendCommand(DSSCommand.createGICommand("RC-RI", "UDB").commandText)
        }

        initializeRegisterTable()

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

    private fun initializeRegisterTable() {
        val tableLayout = findViewById<TableLayout>(R.id.tableData)

        // Clear existing rows except for the header
        val childCount = tableLayout.childCount
        if (childCount > 0) {
            tableLayout.removeViews(0, childCount - 1)
        }

        Log.d("RcRiEmulatorActivity", "Register List Size: ${registerList.size}")

        // Populate table with registers
        for ((index, register) in registerList.withIndex()) {
            val tableRow = TableRow(this)

            val noTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(
                    40.dpToPx(), TableRow.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                text = (index + 1).toString()
            }

            val nameTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                text = register.name
            }

            val valueTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                text = register.getValueString() ?: "null"

                // Enable click feedback
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)

                // Set OnClickListener on the valueTextView
                setOnClickListener {
                    showEditDialog(register, this)
                }
            }


            val directionTextView = TextView(this).apply {
                layoutParams =
                    TableRow.LayoutParams(100.dpToPx(), TableRow.LayoutParams.WRAP_CONTENT)
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                text = register.direction.toString()
            }

            // Add TextViews to TableRow
            tableRow.addView(noTextView)
            tableRow.addView(nameTextView)
            tableRow.addView(valueTextView)
            tableRow.addView(directionTextView)

            tableLayout.addView(tableRow)

            Log.d(
                "UdbEmulatorActivity", "Register: ${register.name}, Value: ${register.getValue()}"
            )
        }
    }

    private fun showEditDialog(register: Register, valueTextView: TextView) {
        val builder = AlertDialog.Builder(this)

        // Inflate the custom layout
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_edit_register, null)

        // Get references to the views
        val nameTextView = dialogView.findViewById<TextView>(R.id.register_name)
        val valueTypeTextView = dialogView.findViewById<TextView>(R.id.register_value_type)
        val descriptionTextView = dialogView.findViewById<TextView>(R.id.register_description)
        val directionTextView = dialogView.findViewById<TextView>(R.id.register_direction)
        val inputEditText = dialogView.findViewById<EditText>(R.id.register_value_input)

        nameTextView.text = "Name: ${register.name}"
        valueTypeTextView.text = "Value Type: ${register.dataType}"
        descriptionTextView.text = "Description: ${register.description}"
        directionTextView.text = "Direction: ${register.direction}"

        // Set the current value
        inputEditText.setText(register.getValue().toString())

        // Set the custom view to the dialog
        builder.setView(dialogView)

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            val newValue = inputEditText.text.toString()
            // Validate and update the register value

            try {
                register.setValueString(newValue)
                // Update the TextView
                valueTextView.text = register.getValueString() ?: "null"
                sendCommand(
                    DSSCommand.createSTCommand(
                        "RC-RI", "UDB", register.name, register.getValue().toString()
                    ).commandText
                );
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

}
