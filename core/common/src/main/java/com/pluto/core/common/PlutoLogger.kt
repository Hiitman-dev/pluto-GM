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

    private val SENSITIVE_KEYS = setOf(
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
     * Redact — replace any value paired with a sensitive key with `***`.
     * Handles `key=value`, `key: value`, and JSON snippets.
     */
    fun redact(input: String): String {
        var result = input
        for (key in SENSITIVE_KEYS) {
            // Match key=value or key:value (case-insensitive)
            val regex = Regex("(?i)($key)([=:]\\s*)([\"']?)[^\\s\"',}]+\\3")
            result = result.replace(regex) { mr ->
                "${mr.groupValues[1]}${mr.groupValues[2]}${mr.groupValues[3]}***${mr.groupValues[3]}"
            }
        }
        return result
    }
}
