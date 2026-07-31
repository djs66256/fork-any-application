package com.djs66256.short_drama.feature.ranking.model

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingDrama
import com.djs66256.short_drama.domain.model.RankingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingUiModelTest {

    @Test
    fun `recommend ui model uses recommendation metric and supporting tags`() {
        val model = sampleDrama(
            playCount = 50090000,
            recommendationScore = 99.6,
        ).toUiModel(rank = 1, rankingType = RankingType.RECOMMEND)

        assertEquals("推荐", model.metricLabel)
        assertEquals("996万", model.metricValue)
        assertEquals(RankingMetricVisual.FLAME, model.metricVisual)
        assertEquals("剧情", model.secondaryText)
        assertEquals(listOf("5009万热度", "99.6万次点赞"), model.detailTags.map { it.text })
        assertNull(model.bookingHintText)
    }

    @Test
    fun `hot ui model adds new drama and rating chips when eligible`() {
        val model = sampleDrama(
            episodeCount = 12,
            rating = 9.3,
            playCount = 76760000,
        ).toUiModel(rank = 2, rankingType = RankingType.HOT)

        assertEquals("热度", model.metricLabel)
        assertEquals("7676万", model.metricValue)
        assertEquals(
            listOf("新剧", "评分9.3", "383.8万收藏", "1304.9万次点赞"),
            model.detailTags.map { it.text },
        )
        assertEquals(RankingDetailTagTone.MINT, model.detailTags[0].tone)
        assertEquals(RankingDetailTagTone.CORAL, model.detailTags[1].tone)
    }

    @Test
    fun `booking ui model exposes hint text and calendar metric`() {
        val model = sampleDrama(
            bookingCount = 1988000,
            episodeCount = 11,
            tags = listOf("群像", "情感"),
        ).toUiModel(rank = 1, rankingType = RankingType.BOOKING)

        assertEquals("期待", model.metricLabel)
        assertEquals("198.8万", model.metricValue)
        assertEquals(RankingMetricVisual.CALENDAR, model.metricVisual)
        assertEquals("剧情 · 群像 · 情感", model.secondaryText)
        assertEquals("预告 · 198.8万人预约 · 预计11月上线", model.bookingHintText)
        assertTrue(model.detailTags.any { it.text == "198.8万人预约" })
    }

    @Test
    fun `poster title chunks Chinese titles into stacked lines`() {
        assertEquals("全族托举\n农门状元\n郎", buildPosterTitle("全族托举农门状元郎"))
    }

    private fun sampleDrama(
        playCount: Int = 1200,
        bookingCount: Int = 10,
        recommendationScore: Double = 88.8,
        episodeCount: Int = 24,
        rating: Double = 0.0,
        tags: List<String> = emptyList(),
    ): RankingDrama = RankingDrama(
        id = "drama-1",
        title = "示例短剧",
        description = "排行卡片描述",
        coverUrl = "https://example.com/drama-1.jpg",
        category = "剧情",
        episodeCount = episodeCount,
        tags = tags,
        rating = rating,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
        contentType = RankingContentType.ALL,
        playCount = playCount,
        bookingCount = bookingCount,
        recommendationScore = recommendationScore,
        isBooked = false,
    )
}
