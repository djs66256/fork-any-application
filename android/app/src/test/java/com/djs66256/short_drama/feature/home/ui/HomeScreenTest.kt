package com.djs66256.short_drama.feature.home.ui

import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `home feed bottom content padding reserves cta height margin and spacing`() {
        assertEquals(88.dp, homeFeedBottomContentPadding())
    }

    @Test
    fun `home feed bottom content padding remains configurable for different cta sizes`() {
        assertEquals(
            124.dp,
            homeFeedBottomContentPadding(
                ctaHeight = 80.dp,
                ctaVerticalMargin = 28.dp,
                extraSpacing = 16.dp,
            ),
        )
    }

    @Test
    fun `current home feed drama follows pager page and guards bounds`() {
        val first = sampleDrama(id = "drama-1", title = "第一页")
        val second = sampleDrama(id = "drama-2", title = "第二页")
        val items = listOf(first, second)

        assertEquals(first, currentHomeFeedDrama(items = items, currentPage = 0))
        assertEquals(second, currentHomeFeedDrama(items = items, currentPage = 1))
        assertEquals(second, currentHomeFeedDrama(items = items, currentPage = 8))
        assertEquals(first, currentHomeFeedDrama(items = items, currentPage = -1))
        assertEquals(null, currentHomeFeedDrama(items = emptyList(), currentPage = 0))
    }

    @Test
    fun `active player state only returns current page state for active drama`() {
        val activeState = PlayerUiState(
            dramaId = "drama-2",
            screenState = PlayerScreenState.PLAYING,
            hasLoadedOnce = true,
        )

        assertEquals(
            activeState,
            activePlayerStateForDrama(
                dramaId = "drama-2",
                currentPage = 1,
                page = 1,
                activeDramaId = "drama-2",
                activePlayerUiState = activeState,
            ),
        )
        assertNull(
            activePlayerStateForDrama(
                dramaId = "drama-1",
                currentPage = 0,
                page = 0,
                activeDramaId = "drama-2",
                activePlayerUiState = activeState,
            ),
        )
        assertNull(
            activePlayerStateForDrama(
                dramaId = "drama-2",
                currentPage = 0,
                page = 1,
                activeDramaId = "drama-2",
                activePlayerUiState = activeState,
            ),
        )
    }

    @Test
    fun `home feed player status copy reflects non playing states`() {
        assertEquals(
            "正在准备视频...",
            homeFeedPlayerStatusCopy(PlayerUiState(screenState = PlayerScreenState.BOOTSTRAPPING)),
        )
        assertEquals(
            "视频已暂停",
            homeFeedPlayerStatusCopy(PlayerUiState(screenState = PlayerScreenState.PAUSED)),
        )
        assertEquals(
            "暂无可播放内容",
            homeFeedPlayerStatusCopy(PlayerUiState(screenState = PlayerScreenState.NO_RESOURCE)),
        )
        assertEquals(
            "播放失败",
            homeFeedPlayerStatusCopy(
                PlayerUiState(
                    screenState = PlayerScreenState.ERROR,
                    errorMessage = "播放失败",
                ),
            ),
        )
        assertNull(homeFeedPlayerStatusCopy(PlayerUiState(screenState = PlayerScreenState.PLAYING)))
    }

    private fun sampleDrama(id: String, title: String): Drama = Drama(
        id = id,
        title = title,
        description = "首页卡片描述",
        coverUrl = "https://example.com/$id.jpg",
        category = "都市",
        episodeCount = 12,
        tags = listOf("逆袭", "甜宠", "爽文"),
        rating = 8.6,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
    )
}
