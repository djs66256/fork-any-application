package com.djs66256.short_drama.feature.menu.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.usecase.GetRecentlyViewedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-05 loadIfNeeded gets session first then transitions to success state`() = runTest {
        val items = listOf(
            sampleRecentlyViewed(dramaId = "drama-1"),
            sampleRecentlyViewed(dramaId = "drama-2", title = "最近在看 2"),
        )
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(items)

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)
            assertFalse(initialState.hasLoaded)
            assertTrue(initialState.items.isEmpty())

            viewModel.loadIfNeeded()
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertFalse(loadingState.hasLoaded)
            assertNull(loadingState.errorMessage)
            assertFalse(loadingState.isRetrying)

            advanceUntilIdle()

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(items, loadedState.items)
            assertNull(loadedState.errorMessage)
            assertTrue(loadedState.hasLoaded)
            assertFalse(loadedState.isRetrying)
            assertFalse(loadedState.isEmpty)
        }

        coVerifyOrder {
            playbackSessionStore.getOrCreateSessionId()
            getRecentlyViewedUseCase("session-123")
        }
    }

    @Test
    fun `T-05 repeated loadIfNeeded only requests once after initial success`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(
            listOf(sampleRecentlyViewed()),
        )

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)

        viewModel.loadIfNeeded()
        advanceUntilIdle()
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        coVerify(exactly = 1) { playbackSessionStore.getOrCreateSessionId() }
        coVerify(exactly = 1) { getRecentlyViewedUseCase("session-123") }
    }

    @Test
    fun `T-06 loadIfNeeded maps empty success into empty state`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(emptyList())

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertNull(state.errorMessage)
        assertTrue(state.hasLoaded)
        assertTrue(state.isEmpty)
        assertFalse(state.isRetrying)
    }

    @Test
    fun `T-06 loadIfNeeded keeps at most three recently viewed items`() = runTest {
        val items = listOf(
            sampleRecentlyViewed(dramaId = "drama-1"),
            sampleRecentlyViewed(dramaId = "drama-2", title = "最近在看 2"),
            sampleRecentlyViewed(dramaId = "drama-3", title = "最近在看 3"),
            sampleRecentlyViewed(dramaId = "drama-4", title = "最近在看 4"),
        )
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Success(items)

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(listOf("drama-1", "drama-2", "drama-3"), viewModel.uiState.value.items.map { it.dramaId })
    }

    @Test
    fun `T-06 loadIfNeeded maps api error into local error state`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } returns ApiResult.Error(
            code = "INTERNAL_ERROR",
            message = "服务异常",
        )

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertEquals("服务异常", state.errorMessage)
        assertTrue(state.hasLoaded)
        assertFalse(state.isRetrying)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `T-06 retry recovers from error and clears retrying flag`() = runTest {
        val recoveredItems = listOf(sampleRecentlyViewed(dramaId = "drama-recovered"))
        val releaseRetry = CompletableDeferred<Unit>()
        var useCaseCalls = 0
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } coAnswers {
            useCaseCalls += 1
            if (useCaseCalls == 1) {
                ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败")
            } else {
                releaseRetry.await()
                ApiResult.Success(recoveredItems)
            }
        }

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()
        assertEquals("首次失败", viewModel.uiState.value.errorMessage)

        viewModel.retry()
        runCurrent()

        val retryingState = viewModel.uiState.value
        assertTrue(retryingState.isLoading)
        assertTrue(retryingState.isRetrying)
        assertNull(retryingState.errorMessage)

        releaseRetry.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(recoveredItems, state.items)
        assertNull(state.errorMessage)
        assertTrue(state.hasLoaded)
        assertFalse(state.isRetrying)
    }

    @Test
    fun `T-06 retry ignores duplicate taps while request is in flight`() = runTest {
        val releaseRetry = CompletableDeferred<Unit>()
        var useCaseCalls = 0
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery { getRecentlyViewedUseCase("session-123") } coAnswers {
            useCaseCalls += 1
            if (useCaseCalls == 1) {
                ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败")
            } else {
                releaseRetry.await()
                ApiResult.Success(listOf(sampleRecentlyViewed(dramaId = "drama-final")))
            }
        }

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.retry()
        viewModel.retry()
        runCurrent()

        coVerify(exactly = 2) { getRecentlyViewedUseCase("session-123") }

        releaseRetry.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("drama-final"), state.items.map { it.dramaId })
        assertNull(state.errorMessage)
    }

    @Test
    fun `T-06 blank dramaId click does not emit playback event`() = runTest {
        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)

        viewModel.events.test {
            viewModel.onRecentlyViewedClick("")
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `T-06 valid dramaId click emits playback event`() = runTest {
        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)

        viewModel.events.test {
            viewModel.onRecentlyViewedClick("drama-123")
            when (val event = awaitItem()) {
                is MenuPanelEvent.OpenPlayback -> assertEquals("drama-123", event.dramaId)
                else -> fail("Expected OpenPlayback event")
            }
        }
    }

    @Test
    fun `T-06 session store exception becomes default error state`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } throws IllegalStateException("boom")

        val viewModel = MenuPanelViewModel(playbackSessionStore, getRecentlyViewedUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("最近在看加载失败，请重试", state.errorMessage)
        assertTrue(state.hasLoaded)
        coVerify(exactly = 0) { getRecentlyViewedUseCase(any()) }
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
