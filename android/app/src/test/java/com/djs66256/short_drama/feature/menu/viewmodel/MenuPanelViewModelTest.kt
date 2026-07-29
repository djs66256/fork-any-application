package com.djs66256.short_drama.feature.menu.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.usecase.GetMessagePreviewUseCase
import com.djs66256.short_drama.domain.usecase.GetRecentlyViewedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuPanelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val playbackSessionStore = mockk<PlaybackSessionStore>()
    private val getRecentlyViewedUseCase = mockk<GetRecentlyViewedUseCase>()
    private val getMessagePreviewUseCase = mockk<GetMessagePreviewUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-05 loadIfNeeded gets session and preview then fills both sections`() = runTest {
        val items = listOf(sampleRecentlyViewed(dramaId = "drama-1"), sampleRecentlyViewed(dramaId = "drama-2"))
        val preview = MessagePreview(
            title = "系统通知",
            summary = "你关注的剧集已更新第 12 集。",
            relativeTime = "2小时前",
        )
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(items)
        coEvery { getMessagePreviewUseCase() } returns ApiResult.Success(preview)

        val viewModel = MenuPanelViewModel(
            playbackSessionStore = playbackSessionStore,
            getRecentlyViewedUseCase = getRecentlyViewedUseCase,
            getMessagePreviewUseCase = getMessagePreviewUseCase,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(items, state.recentlyViewed.items)
        assertTrue(state.recentlyViewed.hasLoaded)
        assertNull(state.recentlyViewed.errorMessage)
        assertEquals(preview, state.messagePreview.preview)
        assertTrue(state.messagePreview.hasLoaded)
        assertNull(state.messagePreview.errorMessage)
        assertEquals("2小时前", state.messagePreview.preview?.relativeTime)

        coVerifyOrder {
            playbackSessionStore.getOrCreateSessionId()
            getRecentlyViewedUseCase("session-123")
        }
        coVerify { getMessagePreviewUseCase() }
    }

    @Test
    fun `T-05 preview empty state does not block recently viewed error state`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "最近在看加载失败",
        )
        coEvery { getMessagePreviewUseCase() } returns ApiResult.Success(null)

        val viewModel = MenuPanelViewModel(
            playbackSessionStore = playbackSessionStore,
            getRecentlyViewedUseCase = getRecentlyViewedUseCase,
            getMessagePreviewUseCase = getMessagePreviewUseCase,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("最近在看加载失败", state.recentlyViewed.errorMessage)
        assertTrue(state.recentlyViewed.hasLoaded)
        assertTrue(state.messagePreview.isEmpty)
        assertNull(state.messagePreview.preview)
        assertNull(state.messagePreview.errorMessage)
    }

    @Test
    fun `T-05 preview error falls back independently from recently viewed success`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(listOf(sampleRecentlyViewed()))
        coEvery { getMessagePreviewUseCase() } returns ApiResult.Exception(IllegalStateException("timeout"))

        val viewModel = MenuPanelViewModel(
            playbackSessionStore = playbackSessionStore,
            getRecentlyViewedUseCase = getRecentlyViewedUseCase,
            getMessagePreviewUseCase = getMessagePreviewUseCase,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recentlyViewed.items.size)
        assertNull(state.recentlyViewed.errorMessage)
        assertEquals("消息预览加载失败，稍后重试", state.messagePreview.errorMessage)
        assertTrue(state.messagePreview.hasLoaded)
        assertNull(state.messagePreview.preview)
    }

    @Test
    fun `T-05 repeated loadIfNeeded only requests once after initial success`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(listOf(sampleRecentlyViewed()))
        coEvery { getMessagePreviewUseCase() } returns ApiResult.Success(null)

        val viewModel = MenuPanelViewModel(
            playbackSessionStore = playbackSessionStore,
            getRecentlyViewedUseCase = getRecentlyViewedUseCase,
            getMessagePreviewUseCase = getMessagePreviewUseCase,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        coVerify(exactly = 1) { playbackSessionStore.getOrCreateSessionId() }
        coVerify(exactly = 1) { getRecentlyViewedUseCase("session-123") }
        coVerify(exactly = 1) { getMessagePreviewUseCase() }
    }

    @Test
    fun `T-06 valid dramaId click emits playback event`() = runTest {
        val viewModel = MenuPanelViewModel(
            playbackSessionStore = playbackSessionStore,
            getRecentlyViewedUseCase = getRecentlyViewedUseCase,
            getMessagePreviewUseCase = getMessagePreviewUseCase,
        )

        viewModel.events.test {
            viewModel.onRecentlyViewedClick("drama-123")
            runCurrent()
            when (val event = awaitItem()) {
                is MenuPanelEvent.OpenPlayback -> assertEquals("drama-123", event.dramaId)
                else -> fail("Expected OpenPlayback event")
            }
        }
    }

    private fun sampleRecentlyViewed(
        dramaId: String = "drama-1",
        title: String = "最近在看 1",
    ): RecentlyViewed {
        return RecentlyViewed(
            dramaId = dramaId,
            title = title,
            coverUrl = null,
            episodeNumber = 12,
            progress = 20.0,
            updatedAt = "2026-07-27T15:20:00.000Z",
        )
    }
}
