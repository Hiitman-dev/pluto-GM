package com.pluto.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOTypography
import kotlinx.coroutines.delay

/**
 * SplashScreen — first-launch splash experience.
 *
 * Shows the PLUTO wordmark over the cosmic background for ~1.2 seconds,
 * then calls [onCompleted] so the nav graph can navigate to Home.
 *
 * Per the spec's "First 5 seconds" requirement: the splash should
 * immediately establish the cosmic identity. No spinner, no spinner
 * spinner — just the brand floating in space.
 */
@Composable
fun SplashScreen(onCompleted: () -> Unit) {
    LaunchedEffect(Unit) {
        // The splash is purely cosmetic — we don't block on any I/O here.
        // Real work (Hilt init, etc.) happens in the Application class.
        delay(1200)
        onCompleted()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())
        Text(
            text = "PLUTO",
            style = PLUTOTypography.brandWordmark,
            color = PLUTOColors.FrostWhite,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
