package com.pluto.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * ResultTest — verifies the Result sealed class behaves per CCloud's
 * util/Result.kt contract.
 */
class ResultTest {

    @Test
    fun `success exposes data and not error`() {
        val r = Result.success(42)
        assertThat(r.isSuccess).isTrue()
        assertThat(r.isError).isFalse()
        assertThat(r.getOrNull()).isEqualTo(42)
        assertThat(r.exceptionOrNull()).isNull()
    }

    @Test
    fun `error exposes exception and not data`() {
        val ex = ApiException.NetworkError()
        val r = Result.error<Int>(ex)
        assertThat(r.isSuccess).isFalse()
        assertThat(r.isError).isTrue()
        assertThat(r.getOrNull()).isNull()
        assertThat(r.exceptionOrNull()).isSameInstanceAs(ex)
    }

    @Test
    fun `map transforms success value`() {
        val r = Result.success(5)
        val mapped = r.map { it * 2 }
        assertThat(mapped.getOrNull()).isEqualTo(10)
    }

    @Test
    fun `map preserves error`() {
        val ex = ApiException.NotFound()
        val r = Result.error<Int>(ex)
        val mapped = r.map { it * 2 }
        assertThat(mapped.isError).isTrue()
        assertThat(mapped.exceptionOrNull()).isSameInstanceAs(ex)
    }

    @Test
    fun `onSuccess callback fires for success only`() {
        var called = 0
        Result.success(1).onSuccess { called++ }
        Result.error<Int>(ApiException.UnknownError()).onSuccess { called++ }
        assertThat(called).isEqualTo(1)
    }

    @Test
    fun `onError callback fires for error only`() {
        var called = 0
        Result.success(1).onError { called++ }
        Result.error<Int>(ApiException.UnknownError()).onError { called++ }
        assertThat(called).isEqualTo(1)
    }

    @Test
    fun `ApiException fromException wraps IOException as NetworkError`() {
        val ex = java.io.IOException("timeout")
        val mapped = ApiException.fromException(ex)
        assertThat(mapped).isInstanceOf(ApiException.NetworkError::class.java)
    }

    @Test
    fun `ApiException fromException preserves ApiException instances`() {
        val original = ApiException.ServerError(500)
        val mapped = ApiException.fromException(original)
        assertThat(mapped).isSameInstanceAs(original)
    }

    @Test
    fun `redact hides API key in URL`() {
        val input = "GET https://example.com/api/genre/all/SECRET_KEY_123"
        val redacted = PlutoLogger.redact(input)
        assertThat(redacted).contains("***")
        assertThat(redacted).doesNotContain("SECRET_KEY_123")
    }

    @Test
    fun `redact hides password in JSON`() {
        val input = """{"email":"a@b.com","password":"hunter2","token":"xyz"}"""
        val redacted = PlutoLogger.redact(input)
        assertThat(redacted).doesNotContain("hunter2")
        assertThat(redacted).doesNotContain("xyz")
    }

    @Test
    fun `redact does not over-redact substrings of legitimate words`() {
        // "monkey" contains "key" but should NOT be redacted
        assertThat(PlutoLogger.redact("monkey=food")).contains("monkey=food")
        // "author" contains "auth" but should NOT be redacted
        assertThat(PlutoLogger.redact("author=alice")).contains("author=alice")
        // "donkey" contains "key" but should NOT be redacted
        assertThat(PlutoLogger.redact("donkey")).isEqualTo("donkey")
        // "keyboard" contains "key" but should NOT be redacted
        assertThat(PlutoLogger.redact("keyboard=input")).contains("keyboard=input")
        // "tokenize" contains "token" but should NOT be redacted
        assertThat(PlutoLogger.redact("tokenize=split")).contains("tokenize=split")
    }

    @Test
    fun `redact preserves non-sensitive content`() {
        val input = "User a@b.com searched for 'batman' at 14:32"
        val redacted = PlutoLogger.redact(input)
        assertThat(redacted).contains("batman")
        assertThat(redacted).contains("14:32")
    }
}
