package com.djs66256.short_drama.feature.theater.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.domain.model.TheaterDrama
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery
import com.djs66256.short_drama.domain.usecase.GetTheaterFeedUseCase
import com.djs66256.short_drama.navigation.TheaterShortcutRoute
import io.mockk.coEvery
import io.mockk.coVerify
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
class TheaterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getTheaterFeedUseCase = mockk<GetTheaterFeedUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-02 default first load succeeds with all first page`() = runTest {
        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(
                channel = TheaterChannel.ALL,
                items = listOf(sampleDrama(id = "drama-1")),
                page = 1,
                totalPages = 2,
            ),
        )

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TheaterChannel.ALL, state.selectedChannel)
        assertEquals(listOf("drama-1"), state.items.map { it.id })
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertTrue(state.hasNextPage)
        coVerify(exactly = 1) { getTheaterFeedUseCase.invoke(defaultQuery()) }
    }

    @Test
    fun `T-03 selecting non all channel shows empty state without error`() = runTest {
        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(
                channel = TheaterChannel.ALL,
                items = listOf(sampleDrama(id = "seed")),
            ),
        )
        coEvery {
            getTheaterFeedUseCase.invoke(TheaterQuery(channel = TheaterChannel.REAL, page = 1, pageSize = 20))
        } returns ApiResult.Success(
            theaterPage(channel = TheaterChannel.REAL, items = emptyList(), page = 1, totalPages = 0),
        )

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        viewModel.onChannelSelected(TheaterChannel.REAL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TheaterChannel.REAL, state.selectedChannel)
        assertTrue(state.items.isEmpty())
        assertNull(state.errorMessage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun `T-04 quick channel switching only consumes latest response`() = runTest {
        val allGate = CompletableDeferred<Unit>()
        val animeGate = CompletableDeferred<Unit>()

        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(channel = TheaterChannel.ALL, items = listOf(sampleDrama(id = "initial"))),
        )
        coEvery {
            getTheaterFeedUseCase.invoke(TheaterQuery(channel = TheaterChannel.REAL, page = 1, pageSize = 20))
        } coAnswers {
            allGate.await()
            ApiResult.Success(
                theaterPage(channel = TheaterChannel.REAL, items = listOf(sampleDrama(id = "stale-real"))),
            )
        }
        coEvery {
            getTheaterFeedUseCase.invoke(TheaterQuery(channel = TheaterChannel.ANIME, page = 1, pageSize = 20))
        } coAnswers {
            animeGate.await()
            ApiResult.Success(
                theaterPage(channel = TheaterChannel.ANIME, items = listOf(sampleDrama(id = "latest-anime"))),
            )
        }

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        val realJob = async { viewModel.onChannelSelected(TheaterChannel.REAL) }
        runCurrent()
        val animeJob = async { viewModel.onChannelSelected(TheaterChannel.ANIME) }
        runCurrent()

        animeGate.complete(Unit)
        advanceUntilIdle()
        allGate.complete(Unit)
        realJob.await()
        animeJob.await()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TheaterChannel.ANIME, state.selectedChannel)
        assertEquals(listOf("latest-anime"), state.items.map { it.id })
    }

    @Test
    fun `T-05 append success and failure keep constraints`() = runTest {
        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(
                channel = TheaterChannel.ALL,
                items = listOf(sampleDrama(id = "page-1")),
                page = 1,
                totalPages = 2,
            ),
        )
        coEvery {
            getTheaterFeedUseCase.invoke(TheaterQuery(channel = TheaterChannel.ALL, page = 2, pageSize = 20))
        } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "分页失败"),
            ApiResult.Success(
                theaterPage(
                    channel = TheaterChannel.ALL,
                    items = listOf(sampleDrama(id = "page-2")),
                    page = 2,
                    totalPages = 2,
                ),
            ),
        )

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        viewModel.loadNextPageIfNeeded()
        advanceUntilIdle()
        assertEquals(listOf("page-1"), viewModel.uiState.value.items.map { it.id })
        assertEquals("分页失败", viewModel.uiState.value.appendErrorMessage)

        viewModel.loadNextPageIfNeeded()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf("page-1", "page-2"), state.items.map { it.id })
        assertNull(state.appendErrorMessage)
        assertEquals(2, state.page)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun `T-08 scan entry only emits local placeholder effect`() = runTest {
        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(channel = TheaterChannel.ALL, items = emptyList(), page = 1, totalPages = 0),
        )

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onScanClick()
            assertEquals(TheaterEffect.ShowMessage("识图功能开发中"), awaitItem())
        }

        coVerify(exactly = 1) { getTheaterFeedUseCase.invoke(defaultQuery()) }
    }

    @Test
    fun `T-06 quick entries and play emit expected routes`() = runTest {
        coEvery { getTheaterFeedUseCase.invoke(defaultQuery()) } returns ApiResult.Success(
            theaterPage(channel = TheaterChannel.ALL, items = listOf(sampleDrama(id = "drama-9"))),
        )

        val viewModel = TheaterViewModel(getTheaterFeedUseCase)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onSearchClick()
            assertEquals(TheaterEffect.Navigate(TheaterShortcutRoute.Search.route), awaitItem())

            viewModel.onShortcutClick(TheaterShortcutRoute.Classification)
            assertEquals(TheaterEffect.Navigate(TheaterShortcutRoute.Classification.route), awaitItem())

            viewModel.onShortcutClick(TheaterShortcutRoute.Ranking)
            assertEquals(TheaterEffect.Navigate(TheaterShortcutRoute.Ranking.route), awaitItem())

            viewModel.onShortcutClick(TheaterShortcutRoute.Booking)
            assertEquals(TheaterEffect.Navigate(TheaterShortcutRoute.Booking.route), awaitItem())

            viewModel.onShortcutClick(TheaterShortcutRoute.NewReleases)
            assertEquals(TheaterEffect.Navigate(TheaterShortcutRoute.NewReleases.route), awaitItem())

            viewModel.onDramaClick("drama-9")
            assertEquals(TheaterEffect.OpenPlay("drama-9"), awaitItem())
        }
    }

    private fun defaultQuery(): TheaterQuery = TheaterQuery(
        channel = TheaterChannel.ALL,
        page = 1,
        pageSize = 20,
    )

    private fun theaterPage(
        channel: TheaterChannel,
        items: List<TheaterDrama>,
        page: Int = 1,
        pageSize: Int = 20,
        totalPages: Int = 1,
    ): TheaterPage = TheaterPage(
        channel = channel,
        items = items,
        page = page,
        pageSize = pageSize,
        total = items.size * totalPages,
        totalPages = totalPages,
    )

    private fun sampleDrama(id: String): TheaterDrama = TheaterDrama(
        id = id,
        title = "示例短剧$id",
        description = "剧场卡片描述",
        coverUrl = "https://example.com/$id.jpg",
        category = "都市",
        episodeCount = 12,
        tags = listOf("逆袭", "豪门"),
        rating = 8.6,
        createdAt = "2026-07-25T00:00:00Z",
        updatedAt = "2026-07-25T00:00:00Z",
        heat = 23000,
    )
}
