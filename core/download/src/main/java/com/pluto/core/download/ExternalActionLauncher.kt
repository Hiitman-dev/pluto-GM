package com.pluto.core.download

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.pluto.core.common.PlutoLogger
import com.pluto.core.model.ExternalApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExternalActionLauncher — opens content in external player / downloader apps.
 *
 * Implements Sections 29-30 of the master spec:
 *   - "Open with..." for video players (MX Player, VLC, KMPlayer)
 *   - "Download with..." for downloaders (ADM, Browser, System)
 *
 * Uses Android Intent resolution. NEVER assumes the application is
 * installed — if unavailable, shows a clean "Not installed" state.
 *
 * DIRECT PORT of CCloud's `utils/DownloadUtils.kt` behavior, with the
 * added "is installed" check the spec requires.
 */
@Singleton
class ExternalActionLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Returns the list of installed external video players. */
    fun getInstalledVideoPlayers(): List<ExternalAppInfo> =
        ExternalApps.VIDEO_PLAYERS.mapNotNull { (name, pkg, mime) ->
            if (isPackageInstalled(pkg)) {
                ExternalAppInfo(name = name, packageName = pkg, mimeType = mime, installed = true)
            } else null
        }

    /** Returns the list of installed external downloaders. */
    fun getInstalledDownloaders(): List<ExternalAppInfo> =
        ExternalApps.DOWNLOADERS.mapNotNull { (name, pkg) ->
            if (isPackageInstalled(pkg)) {
                ExternalAppInfo(name = name, packageName = pkg, mimeType = "*/*", installed = true)
            } else null
        }

    /** Returns ALL configured video players, marking installed = true/false. */
    fun getAllVideoPlayers(): List<ExternalAppInfo> =
        ExternalApps.VIDEO_PLAYERS.map { (name, pkg, mime) ->
            ExternalAppInfo(name = name, packageName = pkg, mimeType = mime, installed = isPackageInstalled(pkg))
        }

    fun getAllDownloaders(): List<ExternalAppInfo> =
        ExternalApps.DOWNLOADERS.map { (name, pkg) ->
            ExternalAppInfo(name = name, packageName = pkg, mimeType = "*/*", installed = isPackageInstalled(pkg))
        }

    /** Open a URL in a specific external video player. */
    fun openWithVideoPlayer(url: String, packageName: String, mimeType: String = "video/*"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mimeType)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-External", "Failed to open with $packageName: ${e.message}")
            toast("$packageName is not installed")
            false
        }
    }

    /** Open a URL in a specific external downloader. */
    fun openWithDownloader(url: String, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-External", "Failed to open with $packageName: ${e.message}")
            toast("$packageName is not installed")
            false
        }
    }

    /** Open a URL in the system browser (no specific app required). */
    fun openInBrowser(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-External", "No browser available: ${e.message}")
            toast("No browser application found")
            false
        }
    }

    /** Open the system "open with..." chooser for a URL. */
    fun openWithChooser(url: String, mimeType: String = "video/*"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-External", "Chooser failed: ${e.message}")
            toast("No compatible application found")
            false
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

data class ExternalAppInfo(
    val name: String,
    val packageName: String,
    val mimeType: String,
    val installed: Boolean
)
