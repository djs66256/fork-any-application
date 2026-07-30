package com.djs66256.short_drama.feature.home.ui

import com.djs66256.short_drama.domain.model.Drama
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenTest {

    @Test
    fun `T-02 menu entry icon content description is stable`() {
        assertEquals("打开菜单", HOME_MENU_ENTRY_CONTENT_DESCRIPTION)
    }

    @Test
    fun `T-07 search entry icon content description is stable`() {
        assertEquals("打开搜索", HOME_SEARCH_ENTRY_CONTENT_DESCRIPTION)
    }

    @Test
    fun `T-05 feed actions only enable navigation for non-blank drama id`() {
        assertTrue(hasNavigableDramaId("drama-001"))
        assertFalse(hasNavigableDramaId(""))
        assertFalse(hasNavigableDramaId("   "))
    }

    @Test
    fun `T-07 comment action only enables navigation for non blank drama id`() {
        assertTrue(hasNavigableDramaId("comment-drama"))
        assertFalse(hasNavigableDramaId(""))
        assertFalse(hasNavigableDramaId("   "))
    }

    @Test
    fun `T-04 check in popup is suppressed when home already has blocking modal`() {
        assertFalse(shouldRenderCheckInPopup(isPopupVisible = true, hasBlockingModal = true))
        assertTrue(shouldRenderCheckInPopup(isPopupVisible = true, hasBlockingModal = false))
        assertFalse(shouldRenderCheckInPopup(isPopupVisible = false, hasBlockingModal = false))
    }

    @Test
    fun `home card meta text includes category tags episodes and rating`() {
        val drama = Drama(
            id = "drama-1",
            title = "示例短剧",
            description = "首页卡片描述",
            coverUrl = "https://example.com/cover.jpg",
            category = "都市",
            episodeCount = 12,
            tags = listOf("逆袭", "甜宠", "爽文"),
            rating = 8.6,
            createdAt = "2026-07-25T00:00:00Z",
            updatedAt = "2026-07-25T00:00:00Z",
        )

        assertEquals("都市  逆袭 · 甜宠 · 爽文  第1集 | 共12集  评分 8.6", buildDramaMeta(drama))
    }

    @Test
    fun `compact count uses wan suffix for large numbers`() {
        assertEquals("470", formatCompactCount(470))
        assertEquals("1.25万", formatCompactCount(12_500))
        assertEquals("40.4万", formatCompactCount(404_000))
    }

    @Test
    fun `frame cta title falls back to at least one episode`() {
        val title = buildFrameCtaTitle(episodeCount = 0)

        assertEquals("观看完整漫剧 · 全1集", title)
    }
}
