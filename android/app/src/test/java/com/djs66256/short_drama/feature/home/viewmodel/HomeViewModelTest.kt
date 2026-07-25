package com.djs66256.short_drama.feature.home.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getDramasUseCase = mockk<GetDramasUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-01 loadIfNeeded transitions from loading to populated feed`() = runTest {
        val dramas = listOf(sampleDrama())
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(dramas)

        val viewModel = HomeViewModel(getDramasUseCase)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertTrue(initialState.items.isEmpty())
            assertFalse(initialState.hasLoadedOnce)

            viewModel.loadIfNeeded()
            advanceUntilIdle()

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(dramas, loadedState.items)
            assertNull(loadedState.errorMessage)
            assertTrue(loadedState.hasLoadedOnce)
            assertFalse(loadedState.isRetrying)
        }

        coVerify(exactly = 1) { getDramasUseCase(page = 1, pageSize = 10) }
    }

    @Test
    fun `T-02 loadIfNeeded transitions to empty state when feed is empty`() = runTest {
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(emptyList())

        val viewModel = HomeViewModel(getDramasUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.isRetrying)

        coVerify(exactly = 1) { getDramasUseCase(page = 1, pageSize = 10) }
    }

    @Test
    fun `T-03 loadIfNeeded transitions to error state on server error`() = runTest {
        coEvery {
            getDramasUseCase(page = 1, pageSize = 10)
        } returns ApiResult.Error(code = "INTERNAL_ERROR", message = "服务异常")

        val viewModel = HomeViewModel(getDramasUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertEquals("服务异常", state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.isRetrying)
    }

    @Test
    fun `T-04 retry recovers from error and emits populated feed`() = runTest {
        val dramas = listOf(sampleDrama(id = "retry-success"))
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败"),
            ApiResult.Success(dramas),
        )

        val viewModel = HomeViewModel(getDramasUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        assertEquals("首次失败", viewModel.uiState.value.errorMessage)

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(dramas, state.items)
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.isRetrying)

        coVerify(exactly = 2) { getDramasUseCase(page = 1, pageSize = 10) }
    }

    @Test
    fun `T-04 retry ignores duplicate taps while request is in flight`() = runTest {
        val releaseRetry = CompletableDeferred<Unit>()
        var invocationCount = 0
        val dramas = listOf(sampleDrama(id = "final-success"))

        coEvery { getDramasUseCase(page = 1, pageSize = 10) } coAnswers {
            invocationCount += 1
            if (invocationCount == 1) {
                ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败")
            } else {
                releaseRetry.await()
                ApiResult.Success(dramas)
            }
        }

        val viewModel = HomeViewModel(getDramasUseCase)
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.retry()
        viewModel.retry()
        runCurrent()

        coVerify(exactly = 2) { getDramasUseCase(page = 1, pageSize = 10) }

        releaseRetry.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(dramas, state.items)
        assertNull(state.errorMessage)
    }

    private fun sampleDrama(id: String = "drama-1"): Drama = Drama(
        id = id,
        title = "示例短剧",
        description = "首页卡片描述",
        coverUrl = "https://example.com/cover.jpg",
        category = "都市",
        episodeCount = 12,
        tags = listOf("逆袭", "甜宠"),
        rating = 8.6,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
    )
}
