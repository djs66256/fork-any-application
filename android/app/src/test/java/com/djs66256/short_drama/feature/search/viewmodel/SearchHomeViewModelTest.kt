package com.djs66256.short_drama.feature.search.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.domain.usecase.ClearSearchHistoryUseCase
import com.djs66256.short_drama.domain.usecase.GetHotSearchKeywordsUseCase
import com.djs66256.short_drama.domain.usecase.ObserveSearchHistoryUseCase
import com.djs66256.short_drama.feature.search.model.SearchQuickEntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class SearchHomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val observeSearchHistoryUseCase = mockk<ObserveSearchHistoryUseCase>()
    private val getHotSearchKeywordsUseCase = mockk<GetHotSearchKeywordsUseCase>()
    private val clearSearchHistoryUseCase = mockk<ClearSearchHistoryUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-02 initialization exposes history and hot searches`() = runTest {
        every { observeSearchHistoryUseCase.invoke() } returns flowOf(
            listOf(SearchHistoryItem(keyword = "逆袭", updatedAtEpochMillis = 1L)),
        )
        coEvery { getHotSearchKeywordsUseCase.invoke() } returns ApiResult.Success(
            listOf(HotSearchItem(rank = 1, keyword = "豪门", score = 999)),
        )

        val viewModel = SearchHomeViewModel(
            observeSearchHistoryUseCase,
            getHotSearchKeywordsUseCase,
            clearSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("逆袭"), state.history.map { it.keyword })
        assertEquals(listOf("豪门"), state.hotSearches.map { it.keyword })
        assertNull(state.hotSearchErrorMessage)
        assertEquals(4, state.quickEntries.size)
    }

    @Test
    fun `T-02 hot search failure is partial error`() = runTest {
        every { observeSearchHistoryUseCase.invoke() } returns flowOf(emptyList())
        coEvery { getHotSearchKeywordsUseCase.invoke() } returns ApiResult.Error(
            code = "INTERNAL_ERROR",
            message = "服务异常",
        )

        val viewModel = SearchHomeViewModel(
            observeSearchHistoryUseCase,
            getHotSearchKeywordsUseCase,
            clearSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hotSearches.isEmpty())
        assertEquals("服务异常", state.hotSearchErrorMessage)
    }

    @Test
    fun `T-02 submit history and quick entry emit navigation events`() = runTest {
        every { observeSearchHistoryUseCase.invoke() } returns flowOf(emptyList())
        coEvery { getHotSearchKeywordsUseCase.invoke() } returns ApiResult.Success(emptyList())

        val viewModel = SearchHomeViewModel(
            observeSearchHistoryUseCase,
            getHotSearchKeywordsUseCase,
            clearSearchHistoryUseCase,
        )
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.submitHistory(" 逆袭 ")
            when (val searchEvent = awaitItem()) {
                is SearchHomeEvent.OpenSearchResult -> {
                    assertEquals("search/result?query=%E9%80%86%E8%A2%AD", searchEvent.route)
                }
                else -> fail("Expected search result event")
            }

            viewModel.openQuickEntry(SearchQuickEntryType.RANKING)
            when (val quickEntryEvent = awaitItem()) {
                is SearchHomeEvent.OpenQuickEntry -> assertEquals("ranking", quickEntryEvent.route)
                else -> fail("Expected quick entry event")
            }

            viewModel.openQuickEntry(SearchQuickEntryType.CLASSIFICATION)
            when (val classificationEvent = awaitItem()) {
                is SearchHomeEvent.OpenQuickEntry -> assertEquals("classification", classificationEvent.route)
                else -> fail("Expected classification quick entry event")
            }
        }
    }

    @Test
    fun `T-02 clear history delegates to use case`() = runTest {
        every { observeSearchHistoryUseCase.invoke() } returns flowOf(emptyList())
        coEvery { getHotSearchKeywordsUseCase.invoke() } returns ApiResult.Success(emptyList())
        coEvery { clearSearchHistoryUseCase.invoke() } returns Unit

        val viewModel = SearchHomeViewModel(
            observeSearchHistoryUseCase,
            getHotSearchKeywordsUseCase,
            clearSearchHistoryUseCase,
        )
        advanceUntilIdle()

        viewModel.clearHistory()
        advanceUntilIdle()

        coVerify(exactly = 1) { clearSearchHistoryUseCase.invoke() }
    }
}
