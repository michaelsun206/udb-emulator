package com.dss.emulator.bluetooth

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class BLEPermissionsManager(
    private val activity: ComponentActivity,
    private val onResult: (Boolean) -> Unit
) {
    private val requiredPermissions: List<String> = buildRequiredPermissions()

    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if all permissions were granted
            val allGranted = permissions.all { it.value }
            onResult(allGranted)
        }

    fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            onResult(true)
        }
    }

    private fun buildRequiredPermissions(): List<String> {
        return Constants.REQUIRED_PERMISSIONS.toMutableList()
    }
}