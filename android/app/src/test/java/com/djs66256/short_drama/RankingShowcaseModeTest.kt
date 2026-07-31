package com.djs66256.short_drama

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingShowcaseModeTest {

    @Test
    fun `parse ranking showcase mode accepts canonical values`() {
        assertEquals(RankingShowcaseMode.HOT, parseRankingShowcaseMode("hot"))
        assertEquals(RankingShowcaseMode.RECOMMEND, parseRankingShowcaseMode("recommend"))
        assertEquals(RankingShowcaseMode.BOOKING, parseRankingShowcaseMode("booking"))
    }

    @Test
    fun `parse ranking showcase mode is case insensitive and trims whitespace`() {
        assertEquals(RankingShowcaseMode.RECOMMEND, parseRankingShowcaseMode("  ReCoMmEnD  "))
    }

    @Test
    fun `parse ranking showcase mode rejects unsupported values`() {
        assertNull(parseRankingShowcaseMode(null))
        assertNull(parseRankingShowcaseMode(""))
        assertNull(parseRankingShowcaseMode("unknown"))
    }
}
