package com.djs66256.short_drama.feature.ranking.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookDramaResult
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingDrama
import com.djs66256.short_drama.domain.model.RankingPage
import com.djs66256.short_drama.domain.model.RankingQuery
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.domain.usecase.BookDramaUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaRankingsUseCase
import com.djs66256.short_drama.navigation.AppDestination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class RankingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getDramaRankingsUseCase = mockk<GetDramaRankingsUseCase>()
    private val bookDramaUseCase = mockk<BookDramaUseCase>()
    private val authSessionProvider = mockk<AuthSessionProvider>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-01 default first load succeeds with all plus hot`() = runTest {
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "drama-1")),
                page = 1,
                totalPages = 2,
            ),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(RankingContentType.ALL, state.selectedContentType)
        assertEquals(RankingType.HOT, state.selectedRankingType)
        assertEquals(1, state.items.size)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.hasLoadedOnce)
        assertTrue(state.hasNextPage)
        assertEquals("热度", state.items.single().metricLabel)
        coVerify(exactly = 1) { getDramaRankingsUseCase.invoke(defaultQuery()) }
    }

    @Test
    fun `T-02 default first load empty enters empty state`() = runTest {
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(items = emptyList(), page = 1, totalPages = 0),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun `T-03 default first load failure and retry recover successfully`() = runTest {
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败"),
            ApiResult.Success(rankingPage(items = listOf(sampleDrama(id = "retry-success")))),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        assertEquals("首次失败", viewModel.uiState.value.errorMessage)

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        coVerify(exactly = 2) { getDramaRankingsUseCase.invoke(defaultQuery()) }
    }

    @Test
    fun `T-04 switching content type keeps current ranking type and refreshes first page`() = runTest {
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(items = listOf(sampleDrama(id = "page-one"), sampleDrama(id = "page-two"))),
        )
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.LIVE_ACTION,
                    type = RankingType.BOOKING,
                    page = 1,
                    pageSize = 10,
                ),
            )
        } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "live-booking", bookingCount = 100, isBooked = false)),
            ),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(mapOf(
                AppDestination.Arg.CONTENT_TYPE to "all",
                AppDestination.Arg.TYPE to "booking",
            )),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        viewModel.onContentTypeSelected(RankingContentType.LIVE_ACTION)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(RankingContentType.LIVE_ACTION, state.selectedContentType)
        assertEquals(RankingType.BOOKING, state.selectedRankingType)
        assertEquals(1, state.page)
        assertEquals(listOf("live-booking"), state.items.map { it.id })
        assertEquals("预约数", state.items.single().metricLabel)
    }

    @Test
    fun `T-05 quick ranking tab switches only consume latest response`() = runTest {
        val recommendGate = CompletableDeferred<Unit>()
        val bookingGate = CompletableDeferred<Unit>()

        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(items = listOf(sampleDrama(id = "initial"))),
        )
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.AI,
                    type = RankingType.RECOMMEND,
                    page = 1,
                    pageSize = 10,
                ),
            )
        } coAnswers {
            recommendGate.await()
            ApiResult.Success(
                rankingPage(
                    items = listOf(sampleDrama(id = "stale-recommend", recommendationScore = 12.3)),
                ),
            )
        }
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.AI,
                    type = RankingType.BOOKING,
                    page = 1,
                    pageSize = 10,
                ),
            )
        } coAnswers {
            bookingGate.await()
            ApiResult.Success(
                rankingPage(
                    items = listOf(sampleDrama(id = "latest-booking", bookingCount = 234)),
                ),
            )
        }
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(mapOf(AppDestination.Arg.CONTENT_TYPE to "ai")),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        val recommendJob = async {
            viewModel.onRankingTypeSelected(RankingType.RECOMMEND)
        }
        runCurrent()
        val bookingJob = async {
            viewModel.onRankingTypeSelected(RankingType.BOOKING)
        }
        runCurrent()

        bookingGate.complete(Unit)
        advanceUntilIdle()
        recommendGate.complete(Unit)
        recommendJob.await()
        bookingJob.await()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(RankingType.BOOKING, state.selectedRankingType)
        assertEquals(listOf("latest-booking"), state.items.map { it.id })
        assertEquals("预约数", state.items.single().metricLabel)
    }

    @Test
    fun `T-06 load next page appends once and ignores duplicate triggers`() = runTest {
        val appendGate = CompletableDeferred<Unit>()
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "page-1")),
                page = 1,
                totalPages = 2,
            ),
        )
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.ALL,
                    type = RankingType.HOT,
                    page = 2,
                    pageSize = 10,
                ),
            )
        } coAnswers {
            appendGate.await()
            ApiResult.Success(
                rankingPage(
                    items = listOf(sampleDrama(id = "page-2")),
                    page = 2,
                    totalPages = 2,
                ),
            )
        }
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        viewModel.loadNextPageIfNeeded()
        viewModel.loadNextPageIfNeeded()
        runCurrent()

        coVerify(exactly = 1) {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.ALL,
                    type = RankingType.HOT,
                    page = 2,
                    pageSize = 10,
                ),
            )
        }

        appendGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("page-1", "page-2"), state.items.map { it.id })
        assertEquals(2, state.page)
        assertFalse(state.hasNextPage)
        assertFalse(state.isAppending)
    }

    @Test
    fun `T-07 append failure keeps loaded items and retry append recovers`() = runTest {
        coEvery { getDramaRankingsUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "page-1")),
                page = 1,
                totalPages = 2,
            ),
        )
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.ALL,
                    type = RankingType.HOT,
                    page = 2,
                    pageSize = 10,
                ),
            )
        } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "分页失败"),
            ApiResult.Success(
                rankingPage(
                    items = listOf(sampleDrama(id = "page-2")),
                    page = 2,
                    totalPages = 2,
                ),
            ),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        viewModel.loadNextPageIfNeeded()
        advanceUntilIdle()

        assertEquals(listOf("page-1"), viewModel.uiState.value.items.map { it.id })
        assertEquals("分页失败", viewModel.uiState.value.appendErrorMessage)

        viewModel.retryAppend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("page-1", "page-2"), state.items.map { it.id })
        assertNull(state.appendErrorMessage)
        assertEquals(2, state.page)
    }

    @Test
    fun `T-08 logged in booking success updates current item in place`() = runTest {
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.ALL,
                    type = RankingType.BOOKING,
                    page = 1,
                    pageSize = 10,
                ),
            )
        } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "drama-1", bookingCount = 88, isBooked = false)),
            ),
        )
        every { authSessionProvider.isLoggedIn() } returns true
        coEvery { bookDramaUseCase.invoke("drama-1") } returns ApiResult.Success(
            BookDramaResult(
                dramaId = "drama-1",
                booked = true,
                bookingCount = 89,
            ),
        )

        val viewModel = RankingViewModel(
            SavedStateHandle(mapOf(AppDestination.Arg.TYPE to "booking")),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        viewModel.onBookClick("drama-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.items.single().isBooked)
        assertEquals(89, state.items.single().bookingCount)
        assertFalse("drama-1" in state.bookingInFlightIds)
        coVerify(exactly = 1) { bookDramaUseCase.invoke("drama-1") }
    }

    @Test
    fun `T-09 anonymous booking emits require login and skips api call`() = runTest {
        coEvery {
            getDramaRankingsUseCase.invoke(
                RankingQuery(
                    contentType = RankingContentType.ALL,
                    type = RankingType.BOOKING,
                    page = 1,
                    pageSize = 10,
                ),
            )
        } returns ApiResult.Success(
            rankingPage(
                items = listOf(sampleDrama(id = "drama-1", bookingCount = 88, isBooked = false)),
            ),
        )
        every { authSessionProvider.isLoggedIn() } returns false

        val viewModel = RankingViewModel(
            SavedStateHandle(mapOf(AppDestination.Arg.TYPE to "booking")),
            getDramaRankingsUseCase,
            bookDramaUseCase,
            authSessionProvider,
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onBookClick("drama-1")
            assertEquals(
                RankingEffect.RequireLogin("ranking?contentType=all&type=booking"),
                awaitItem(),
            )
        }

        coVerify(exactly = 0) { bookDramaUseCase.invoke(any()) }
    }

    private fun defaultQuery(): RankingQuery = RankingQuery(
        contentType = RankingContentType.ALL,
        type = RankingType.HOT,
        page = 1,
        pageSize = 10,
    )

    private fun rankingPage(
        items: List<RankingDrama>,
        page: Int = 1,
        pageSize: Int = 10,
        totalPages: Int = 1,
    ): RankingPage = RankingPage(
        items = items,
        page = page,
        pageSize = pageSize,
        total = items.size * totalPages,
        totalPages = totalPages,
    )

    private fun sampleDrama(
        id: String,
        bookingCount: Int = 10,
        recommendationScore: Double = 88.8,
        isBooked: Boolean = false,
    ): RankingDrama = RankingDrama(
        id = id,
        title = "示例短剧$id",
        description = "排行卡片描述",
        coverUrl = "https://example.com/$id.jpg",
        category = "都市",
        episodeCount = 12,
        tags = listOf("逆袭", "甜宠"),
        rating = 8.6,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
        contentType = RankingContentType.AI,
        playCount = 1200,
        bookingCount = bookingCount,
        recommendationScore = recommendationScore,
        isBooked = isBooked,
    )
}
