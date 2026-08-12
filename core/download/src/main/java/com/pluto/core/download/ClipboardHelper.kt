package com.pluto.core.download

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.pluto.core.common.PlutoLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ClipboardHelper — copy one or many URLs to the clipboard.
 *
 * Implements Section 27 ("COPY LINKS") of the master spec:
 *   - Copy 480p / 720p / 1080p / Copy All Links
 *   - For Copy All: copy only links that actually exist.
 *   - Show "LINKS COPIED" with subtle animated confirmation.
 */
@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun copyLink(url: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("PLUTO Video URL", url))
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-Clipboard", "Copy failed: ${e.message}")
            false
        }
    }

    /**
     * Copy multiple links at once, joined by newlines.
     * Empty / blank URLs are skipped (per Section 27 spec).
     */
    fun copyMultipleLinks(urls: List<String>): Boolean {
        val valid = urls.filter { it.isNotBlank() }
        if (valid.isEmpty()) return false
        val joined = valid.joinToString("\n")
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("PLUTO Video URLs", joined))
            true
        } catch (e: Exception) {
            PlutoLogger.w("PLUTO-Clipboard", "Multi-copy failed: ${e.message}")
            false
        }
    }
}
