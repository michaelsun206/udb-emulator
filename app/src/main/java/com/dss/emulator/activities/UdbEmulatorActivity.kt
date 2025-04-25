package com.dss.emulator.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.dss.emulator.bluetooth.BLEPermissionsManager
import com.dss.emulator.bluetooth.DataQueueManager
import com.dss.emulator.bluetooth.peripheral.BLEPeripheralController
import com.dss.emulator.core.UDBEmulator
import com.dss.emulator.dsscommand.DSSCommand
import com.dss.emulator.register.Direction
import com.dss.emulator.register.Register
import com.dss.emulator.register.Registers
import com.dss.emulator.register.registerList
import com.dss.emulator.udb.R

class UdbEmulatorActivity : ComponentActivity() {


    private lateinit var historyTextView: TextView
    private lateinit var permissionsManager: BLEPermissionsManager
    private lateinit var bleCentralController: BLEPeripheralController
    private lateinit var udbEmulator: UDBEmulator
    private lateinit var dataQueueManager: DataQueueManager

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_udb_emulator)

        // Initialize and start DataQueueManager
        dataQueueManager = DataQueueManager.getInstance()
        dataQueueManager.start()

        updateRegisterTable()
        historyTextView = findViewById(R.id.historyTextView)
        historyTextView.text = ""

        val toggleButton = findViewById<Button>(R.id.toggleButton)
        val tableContainer = findViewById<LinearLayout>(R.id.tableContainer)

        var isExpanded = false

        toggleButton.setOnClickListener {
            if (isExpanded) {
                tableContainer.visibility = View.GONE
                toggleButton.text = "Show Registers"
            } else {
                tableContainer.visibility = View.VISIBLE
                toggleButton.text = "Hide Registers"
            }
            isExpanded = !isExpanded
        }

        val statusText = this.findViewById(R.id.statusText) as TextView

        permissionsManager = BLEPermissionsManager(this) { granted ->
            if (!granted) {
                Log.e("Permissions", "Bluetooth permissions denied")
                // Alert and Exit
                AlertDialog.Builder(this).setTitle("Bluetooth Permissions Denied")
                    .setMessage("Please grant Bluetooth permissions to use this app.")
                    .setPositiveButton("OK") { _, _ -> finish() }.show()
            } else {
                // Permissions granted, proceed to initialize UI
            }
        }

        permissionsManager.checkAndRequestPermissions()

        bleCentralController = BLEPeripheralController(this, onDeviceConnected = { device ->
            if (device == null) {
                bleCentralController.startAdvertising()
                dataQueueManager.pause() // Pause queue when disconnected

                runOnUiThread {
                    statusText.text = "Waiting For Connection..."
                }
            } else {
                bleCentralController.stopAdvertising()
                dataQueueManager.resume() // Resume queue when connected

                runOnUiThread {
                    statusText.text = "Device: ${device.address}"
                    AlertDialog.Builder(this).setTitle("Device Connected")
                        .setMessage("Device: ${device.address}").setPositiveButton("OK") { _, _ -> }
                        .show()
                }
            }
        })
        bleCentralController.startAdvertising()

        udbEmulator = UDBEmulator(this, bleCentralController)

        // Add listener to handle incoming data
        dataQueueManager.addListener { data ->
            udbEmulator.onReceiveData(data)

            updateHistoryTextView()
            updateRegisterTable()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!dataQueueManager.isActive()) {
            dataQueueManager.start()
        }
        dataQueueManager.resume()
    }

    override fun onPause() {
        super.onPause()
        dataQueueManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataQueueManager.stop()
    }

    private fun updateRegisterTable() {
        runOnUiThread {
            val tableLayout = findViewById<TableLayout>(R.id.tableData)

            // Clear existing rows except for the header
            val childCount = tableLayout.childCount
            if (childCount > 0) tableLayout.removeViews(0, childCount)

            Log.d("UdbEmulatorActivity", "Register List Size: ${registerList.size}")

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
                        if (register.direction == Direction.BOTH || register.direction == Direction.UDB_TO_GUI) {
                            showEditDialog(
                                register, this
                            )
                        } else {
                            Toast.makeText(
                                this@UdbEmulatorActivity,
                                "${register.name} is not editable",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                    "UdbEmulatorActivity",
                    "Register: ${register.name}, Value: ${register.getValue()}"
                )
            }
        }
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun updateHistoryTextView() {
        runOnUiThread {
            historyTextView.text = udbEmulator.getCommandHistory()
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

                Log.d(
                    "UdbEmulatorActivity",
                    "Register: ${register.name}, Value: ${register.getValue()}"
                )
                val rMap = 1L shl register.regMapBit

                Registers.REG_MAP.setValue(rMap)
                sendCommand(DSSCommand.createRMCommand("UDB", "RC-RI", rMap))

                updateRegisterTable()
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

    private fun sendCommand(command: DSSCommand) {
        this.udbEmulator.sendCommand(command)
        updateHistoryTextView()
    }
}