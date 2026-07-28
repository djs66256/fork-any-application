package com.djs66256.short_drama.feature.theater.model

import com.djs66256.short_drama.domain.model.TheaterDrama
import org.junit.Assert.assertEquals
import org.junit.Test

class TheaterDramaItemUiModelTest {

    @Test
    fun `T-07 theater ui model formats heat and meta correctly`() {
        val model = sampleDrama(heat = 23000).toUiModel()

        assertEquals("2.3万", model.heatText)
        assertEquals("都市 · 逆袭 / 豪门 · 68 集 · 评分 8.9", model.metaText)
        assertEquals("https://example.com/drama.jpg", model.coverUrl)
    }

    @Test
    fun `T-07 theater ui model falls back for blank cover and small heat`() {
        val model = sampleDrama(coverUrl = "", heat = 9999, rating = 0.0, tags = emptyList()).toUiModel()

        assertEquals("9999", model.heatText)
        assertEquals("都市 · 68 集", model.metaText)
        assertEquals("", model.coverUrl)
    }

    private fun sampleDrama(
        coverUrl: String = "https://example.com/drama.jpg",
        heat: Int = 98210,
        rating: Double = 8.9,
        tags: List<String> = listOf("逆袭", "豪门"),
    ): TheaterDrama = TheaterDrama(
        id = "drama-1",
        title = "逆袭归来后我成了豪门团宠",
        description = "落魄千金重回豪门，在误会与守护中逆风翻盘。",
        coverUrl = coverUrl,
        category = "都市",
        episodeCount = 68,
        tags = tags,
        rating = rating,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
        heat = heat,
    )
}
