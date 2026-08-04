package com.djs66256.short_drama.feature.ranking.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingScreenTest {

    @Test
    fun `ranking header top padding only keeps local spacing because status bar is handled by modifier`() {
        assertEquals(8.dp, rankingHeaderTopPadding())
        assertEquals(4.dp, rankingHeaderTopPadding(extraSpacing = 4.dp))
    }

    @Test
    fun `ranking list bottom padding keeps lightweight spacing because navigation bar inset is handled by modifier`() {
        assertEquals(20.dp, rankingListBottomPadding())
        assertEquals(12.dp, rankingListBottomPadding(extraSpacing = 12.dp))
    }
}
