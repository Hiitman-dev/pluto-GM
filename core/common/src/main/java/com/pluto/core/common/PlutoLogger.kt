package com.pluto.core.common

/**
 * LogLevel — controls logging behavior across build types.
 *
 * Per Section 91 of the master spec: debug builds get detailed logs,
 * release builds get minimal logs, and credentials are NEVER logged.
 */
enum class LogLevel { VERBOSE, NORMAL, MINIMAL }

/**
 * PlutoLogger — controlled, privacy-respecting logging.
 *
 * In debug builds: full logs (VERBOSE).
 * In release builds: only warnings and errors (MINIMAL).
 *
 * The [redact] helper ensures API keys, passwords, and tokens never
 * appear in logcat.
 */
object PlutoLogger {
    @Volatile
    var level: LogLevel = LogLevel.VERBOSE

    private val SENSITIVE_KEYS = listOf(
        "api_key", "apikey", "api-key",
        "password", "passwd", "pwd",
        "token", "auth", "authorization",
        "secret", "private_key", "key"
    )

    fun d(tag: String, message: String) {
        if (level == LogLevel.VERBOSE) android.util.Log.d(tag, redact(message))
    }

    fun i(tag: String, message: String) {
        if (level != LogLevel.MINIMAL) android.util.Log.i(tag, redact(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.w(tag, redact(message), throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, redact(message), throwable)
    }

    /**
     * Redact — replace sensitive values in logs with `***`.
     *
     * Handles three patterns:
     *   1. `key=value` or `key: value` (with optional quotes around value)
     *      e.g. `password=hunter2`, `"token":"xyz"`, `api-key: secret`
     *   2. URL path segments that look like opaque API tokens
     *      (alphanumeric, length >= 12, no dots) — covers CCloud-style
     *      `/api/.../{API_KEY}` URLs.
     *   3. Bearer tokens in Authorization headers.
     *
     * Word boundaries (`\b`) are used around sensitive keys so that
     * legitimate words containing "key" (e.g. "monkey", "keyboard") or
     * "auth" (e.g. "author") are NOT redacted.
     */
    fun redact(input: String): String {
        var result = input

        // Pattern 1: key=value / key:value (case-insensitive key).
        // The value is delimited by: whitespace, comma, brace, quote, or EOL.
        // Word boundaries around the key prevent over-redaction of substrings
        // like "monkey" or "keyboard".
        for (key in SENSITIVE_KEYS) {
            val escapedKey = Regex.escape(key)
            // Quoted value: key":"value"  or  key":"value"
            val quotedRegex = Regex("(?i)(\\b$escapedKey\\b)([\"']?\\s*[:=]\\s*[\"'])([^\"'\\s,}]+)([\"'])")
            result = result.replace(quotedRegex) { mr ->
                "${mr.groupValues[1]}${mr.groupValues[2]}***${mr.groupValues[4]}"
            }
            // Unquoted value: key=value  or  key: value
            val unquotedRegex = Regex("(?i)(\\b$escapedKey\\b)(\\s*[:=]\\s*)([^\\s,\"'{}]+)")
            result = result.replace(unquotedRegex) { mr ->
                "${mr.groupValues[1]}${mr.groupValues[2]}***"
            }
        }

        // Pattern 2: opaque URL path segments (CCloud-style /api/.../{API_KEY}).
        // Matches a 12+ character alphanumeric token in a URL path that follows
        // /api/. We only redact the token itself, not the rest of the URL.
        val urlTokenRegex = Regex("(/api/[A-Za-z0-9_/]+/)([A-Za-z0-9_\\-]{12,})([/\\s?#]|$)")
        result = result.replace(urlTokenRegex) { mr ->
            "${mr.groupValues[1]}***${mr.groupValues[3]}"
        }

        // Pattern 3: Bearer tokens in Authorization headers.
        val bearerRegex = Regex("(?i)(Authorization\\s*[:]\\s*Bearer\\s+)([A-Za-z0-9_\\-\\.]+)")
        result = result.replace(bearerRegex) { mr ->
            "${mr.groupValues[1]}***"
        }

        return result
    }
}
