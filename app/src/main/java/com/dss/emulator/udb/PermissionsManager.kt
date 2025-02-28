// File: PermissionsManager.kt
package com.dss.emulator.udb

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class PermissionsManager(
    private val context: Context,
    private val onResult: (Boolean) -> Unit
) {
    private val activity = context as ComponentActivity

    private val requestPermissionLauncher = activity.registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        onResult(granted)
    }

    fun checkAndRequestPermissions() {
        val missingPermissions = Constants.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            onResult(true)
        }

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT))
        }
    }
}