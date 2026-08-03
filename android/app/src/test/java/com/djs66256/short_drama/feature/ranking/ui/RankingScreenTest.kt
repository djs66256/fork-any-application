package com.djs66256.short_drama.feature.ranking.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingScreenTest {

    @Test
    fun `ranking header top padding keeps minimum touch safe spacing`() {
        assertEquals(18.dp, rankingHeaderTopPadding(0.dp))
        assertEquals(18.dp, rankingHeaderTopPadding(12.dp))
        assertEquals(28.dp, rankingHeaderTopPadding(28.dp))
    }
}
