package com.trippin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.trippin.app.TrippinApplication
import com.trippin.app.ui.navigation.TrippinNavHost
import com.trippin.app.ui.theme.TrippinTheme
import com.trippin.app.util.PermissionsHelper

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            val app = application as TrippinApplication
            app.carConnectionMonitor.onPermissionsGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TrippinApplication

        requestPermissionsIfNeeded()

        if (intent.getBooleanExtra(EXTRA_START_TRACKING, false)) {
            app.carConnectionMonitor.checkAndStartIfConnected(fromUserAction = true)
        }

        setContent {
            TrippinTheme {
                TrippinNavHost(container = app.container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as TrippinApplication
        if (PermissionsHelper.hasAllRequired(this)) {
            app.carConnectionMonitor.checkAndStartIfConnected(fromUserAction = true)
        } else {
            requestPermissionsIfNeeded()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val missing = PermissionsHelper.requiredPermissions().filter { permission ->
            checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    companion object {
        const val EXTRA_START_TRACKING = "extra_start_tracking"
    }
}
