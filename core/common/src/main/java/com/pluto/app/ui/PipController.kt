package com.pluto.app.ui

/**
 * PipController — bridge between MainActivity's PiP support and the
 * Compose Player screen.
 *
 * Lives in `:core:common` (under the historical package `com.pluto.app.ui`
 * for source-level stability) so that both `:app` (which implements it
 * in MainActivity) and `:feature:player` (which consumes it via the
 * PlayerScreen signature) can depend on it without a circular module
 * graph.
 *
 * The Player screen calls [setWantsPip](true) when playback is active;
 * MainActivity enters PiP on user-leave-hint if the flag is set.
 * [enterPipNow] can be called directly by an in-app PiP button.
 */
interface PipController {
    /** Set whether the player wants to enter PiP when the user leaves. */
    fun setWantsPip(wants: Boolean)

    /** Try to enter PiP right now. Returns false if PiP is unavailable. */
    fun enterPipNow(): Boolean

    /** True if the activity is currently in PiP mode. */
    fun isInPip(): Boolean
}
