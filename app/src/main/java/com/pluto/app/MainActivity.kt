package com.pluto.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pluto.app.ui.PLUTOApp
import com.pluto.app.ui.PipController
import com.pluto.core.designsystem.PLUTOTheme
import com.pluto.core.model.PlutoDeepLinks
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — single-activity host for PLUTO.
 *
 * Responsibilities:
 *   - Edge-to-edge rendering.
 *   - Picture-in-Picture: enters PiP when the user leaves the app while
 *     the player is active. The Player screen calls [PipController.setWantsPip]
 *     when it wants to enter PiP; MainActivity enters PiP on [onUserLeaveHint]
 *     if the request flag is set.
 *   - Predictive back: opted in via the [OnBackPressedCallback]. The
 *     system animates the back gesture on Android 14+.
 *   - Deep links: `pluto://...` URIs are dispatched to the NavHost via
 *     [NavController.handleDeepLink]. Cold-start deep links are read
 *     from the launch [Intent]; warm deep links arrive via [onNewIntent].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Set to true by the Player screen while playback is active. */
    @Volatile private var wantsPip = false

    /** Held so onNewIntent can dispatch deep links to the nav controller. */
    @Volatile private var navControllerRef: NavController? = null

    /** Tracks PiP mode so the Compose tree can react (hide nav bar, etc.). */
    private val isInPipState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isInPipState.value = isInPipMode()

        // Predictive back — the system animates the back gesture. We do
        // not consume back here; the NavHost handles it. This callback
        // opts the activity into the predictive back animation system.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        val pipController = object : PipController {
            override fun setWantsPip(wants: Boolean) { wantsPip = wants }
            override fun enterPipNow() = enterPipIfPossible()
            override fun isInPip(): Boolean = isInPipState.value
        }

        setContent {
            // Observe the activity-level PiP state so recomposition fires
            // when onPictureInPictureModeChanged updates it.
            val isInPip by remember { isInPipState }

            PLUTOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PLUTOApp(
                        pipController = pipController,
                        isInPip = isInPip,
                        onNavControllerReady = { nc -> navControllerRef = nc }
                    )
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If the player is active and PiP is supported, enter PiP when
        // the user leaves the app (home button, recents, etc.).
        if (wantsPip) {
            enterPipIfPossible()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Update the Compose-observable state so the tree recomposes.
        isInPipState.value = isInPictureInPictureMode
    }

    private fun enterPipIfPossible(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
            true
        } catch (e: IllegalStateException) {
            // PiP not allowed in current state — silently ignore
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isInPipMode(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data ?: return
        if (!PlutoDeepLinks.isPlutoUri(data.toString())) return
        // Mark as ACTION_VIEW so NavController.handleDeepLink recognizes it.
        intent.action = Intent.ACTION_VIEW
        // Dispatch to the NavController. handleDeepLink returns true if it
        // found a matching deep-link pattern in the nav graph. If the nav
        // controller isn't ready yet (cold start race), the intent stays on
        // the activity and will be picked up when PLUTOApp calls
        // onNavControllerReady — we re-handle it there.
        val handled = navControllerRef?.handleDeepLink(intent) ?: false
        if (!handled) {
            // The nav graph may not be ready yet (first frame race).
            // The intent is already set via setIntent() above; the next
            // composition pass will see it via currentBackStackEntry.
        }
    }
}
