package com.dss.emulator.udb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dss.emulator.udb.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showModeSelectionDialog()
    }

    private fun showModeSelectionDialog() {
        val options = arrayOf("UDB Emulator", "RC/RI Emulator")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Application Mode")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> startActivity(Intent(this, UdbEmulatorActivity::class.java))
                1 -> startActivity(Intent(this, RcRiEmulatorActivity::class.java))
            }
            finish() // Close MainActivity after selection
        }

        builder.setCancelable(false)
        builder.show()
    }
}
