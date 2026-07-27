package com.djs66256.short_drama.data.local

import com.djs66256.short_drama.domain.model.SearchHistoryItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryLocalDataSourceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `T-05 mergeSearchHistory trims deduplicates and keeps newest first`() {
        val currentItems = listOf(
            SearchHistoryItem(keyword = "都市", updatedAtEpochMillis = 200L),
            SearchHistoryItem(keyword = "逆袭", updatedAtEpochMillis = 100L),
        )

        val merged = mergeSearchHistory(
            currentItems = currentItems,
            newKeyword = " 逆袭 ",
            nowEpochMillis = 300L,
        )

        assertEquals(listOf("逆袭", "都市"), merged.map(SearchHistoryItem::keyword))
        assertEquals(300L, merged.first().updatedAtEpochMillis)
    }

    @Test
    fun `T-05 mergeSearchHistory caps list at ten entries`() {
        val currentItems = (1..10).map { index ->
            SearchHistoryItem(keyword = "词$index", updatedAtEpochMillis = (100 - index).toLong())
        }

        val merged = mergeSearchHistory(
            currentItems = currentItems,
            newKeyword = "新增词",
            nowEpochMillis = 1000L,
        )

        assertEquals(10, merged.size)
        assertEquals("新增词", merged.first().keyword)
        assertTrue(merged.none { it.keyword == "词10" })
    }

    @Test
    fun `T-05 decodeSearchHistory falls back to empty list on malformed payload`() {
        val decoded = decodeSearchHistory("not-json", json)
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `T-05 encodeSearchHistory normalizes payload for clear action`() {
        val encoded = encodeSearchHistory(emptyList(), json)
        assertEquals("[]", encoded)
    }
}
