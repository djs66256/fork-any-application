package com.djs66256.short_drama.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.usecase.SaveSearchHistoryUseCase
import com.djs66256.short_drama.domain.usecase.SearchDramasUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchResultViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val searchDramasUseCase = mockk<SearchDramasUseCase>()
    private val saveSearchHistoryUseCase = mockk<SaveSearchHistoryUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-03 success loads items and writes history`() = runTest {
        coEvery { searchDramasUseCase.invoke("逆袭", 1, 10) } returns ApiResult.Success(
            listOf(sampleDrama()),
        )
        coEvery { saveSearchHistoryUseCase.invoke("逆袭") } returns Unit

        val viewModel = SearchResultViewModel(
            SavedStateHandle(mapOf("query" to "逆袭")),
            searchDramasUseCase,
            saveSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("逆袭", state.query)
        assertEquals(1, state.items.size)
        assertFalse(state.isLoading)
        assertTrue(state.hasLoadedOnce)
        coVerify(exactly = 1) { saveSearchHistoryUseCase.invoke("逆袭") }
    }

    @Test
    fun `T-03 empty result still writes history`() = runTest {
        coEvery { searchDramasUseCase.invoke("豪门", 1, 10) } returns ApiResult.Success(emptyList())
        coEvery { saveSearchHistoryUseCase.invoke("豪门") } returns Unit

        val viewModel = SearchResultViewModel(
            SavedStateHandle(mapOf("query" to "豪门")),
            searchDramasUseCase,
            saveSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertTrue(state.hasLoadedOnce)
        coVerify(exactly = 1) { saveSearchHistoryUseCase.invoke("豪门") }
    }

    @Test
    fun `T-04 failure keeps error state and does not write history`() = runTest {
        coEvery { searchDramasUseCase.invoke("逆袭", 1, 10) } returns ApiResult.Error(
            code = "INTERNAL_ERROR",
            message = "服务异常",
        )

        val viewModel = SearchResultViewModel(
            SavedStateHandle(mapOf("query" to "逆袭")),
            searchDramasUseCase,
            saveSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("服务异常", state.errorMessage)
        assertTrue(state.items.isEmpty())
        coVerify(exactly = 0) { saveSearchHistoryUseCase.invoke(any()) }
    }

    @Test
    fun `T-04 missing query enters invalid state without request`() = runTest {
        val viewModel = SearchResultViewModel(
            SavedStateHandle(),
            searchDramasUseCase,
            saveSearchHistoryUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchResultViewModel.INVALID_QUERY_MESSAGE, state.errorMessage)
        coVerify(exactly = 0) { searchDramasUseCase.invoke(any(), any(), any()) }
    }

    private fun sampleDrama(): Drama = Drama(
        id = "drama-1",
        title = "逆袭人生",
        description = "desc",
        coverUrl = "cover",
        category = "都市",
        episodeCount = 12,
        tags = listOf("逆袭"),
        rating = 8.2,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
    )
}
