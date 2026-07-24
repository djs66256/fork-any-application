package com.djs66256.short_drama.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ApiResultTest {

    @Test
    fun `T-02 Success branch holds data correctly`() {
        val result = ApiResult.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `T-02 Error branch holds code and message correctly`() {
        val result = ApiResult.Error("404", "Not Found")
        assertEquals("404", result.code)
        assertEquals("Not Found", result.message)
    }

    @Test
    fun `T-02 Exception branch holds throwable correctly`() {
        val exception = RuntimeException("boom")
        val result = ApiResult.Exception(exception)
        assertSame(exception, result.throwable)
        assertEquals("boom", result.throwable.message)
    }

    @Test
    fun `T-02 pattern matching on sealed class works correctly`() {
        val results: List<ApiResult<*>> = listOf(
            ApiResult.Success(42),
            ApiResult.Error("500", "Server Error"),
            ApiResult.Exception(IllegalStateException("unexpected"))
        )

        val successCount = results.count { it is ApiResult.Success }
        val errorCount = results.count { it is ApiResult.Error }
        val exceptionCount = results.count { it is ApiResult.Exception }

        assertEquals(1, successCount)
        assertEquals(1, errorCount)
        assertEquals(1, exceptionCount)
    }
}
