package com.djs66256.short_drama.feature.ranking.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingShowcaseModeTest {

    @Test
    fun `debug build enables hot showcase mode`() {
        assertEquals(
            RankingShowcaseMode.HOT,
            resolveRankingShowcaseMode(rawMode = "hot", isDebug = true),
        )
    }

    @Test
    fun `debug build enables booking showcase mode with surrounding spaces`() {
        assertEquals(
            RankingShowcaseMode.BOOKING,
            resolveRankingShowcaseMode(rawMode = " booking ", isDebug = true),
        )
    }

    @Test
    fun `release path never enters showcase mode`() {
        assertNull(resolveRankingShowcaseMode(rawMode = "hot", isDebug = false))
    }

    @Test
    fun `unknown showcase mode falls back to real navigation path`() {
        assertNull(resolveRankingShowcaseMode(rawMode = "unknown", isDebug = true))
    }
}
