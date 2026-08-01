package com.trippin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trippin.app.TrippinApplication
import com.trippin.app.ui.navigation.TrippinNavHost
import com.trippin.app.ui.theme.TrippinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TrippinApplication).container

        setContent {
            TrippinTheme {
                TrippinNavHost(container = container)
            }
        }
    }
}
