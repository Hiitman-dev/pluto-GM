package com.pluto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pluto.core.designsystem.PLUTOTheme
import com.pluto.app.ui.PLUTOApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — single-activity host for PLUTO.
 *
 * Uses edge-to-edge rendering (per Section 69 of the master spec —
 * responsive design). The actual UI lives in [PLUTOApp].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PLUTOTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    PLUTOApp()
                }
            }
        }
    }
}
