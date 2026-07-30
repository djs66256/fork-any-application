package com.djs66256.short_drama.feature.booking.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.model.BookingAsset
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.domain.model.BookingAssetSummary
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.usecase.GetBookingAssetsUseCase
import com.djs66256.short_drama.feature.booking.model.BookingAssetsEffect
import com.djs66256.short_drama.feature.booking.model.BookingAuthGate
import com.djs66256.short_drama.navigation.AppDestination
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingAssetsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getBookingAssetsUseCase = mockk<GetBookingAssetsUseCase>()
    private val authStateHolder = AuthStateHolder(FakeBookingAuthSessionStore())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-11 anonymous state shows login gate and emits menu booking return route`() = runTest {
        val viewModel = BookingAssetsViewModel(getBookingAssetsUseCase, authStateHolder)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showLoginGate)
        assertEquals(BookingAuthGate.Anonymous, state.authGate)
        assertEquals(BookingAssetStatus.ONLINE, state.selectedStatus)

        viewModel.effects.test {
            viewModel.onLoginClick()
            assertEquals(
                BookingAssetsEffect.RequireLogin(AppDestination.menuBooking()),
                awaitItem(),
            )
        }

        coVerify(exactly = 0) { getBookingAssetsUseCase.invoke(any()) }
    }

    @Test
    fun `T-11 authenticated first load maps summary items and pagination`() = runTest {
        val firstPage = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 1, pageSize = 20)
        coEvery { getBookingAssetsUseCase.invoke(firstPage) } returns ApiResult.Success(
            bookingPage(
                status = BookingAssetStatus.ONLINE,
                ids = listOf("online-1"),
                page = 1,
                totalPages = 2,
                summary = BookingAssetSummary(onlineCount = 3, upcomingCount = 5),
            ),
        )

        val viewModel = BookingAssetsViewModel(getBookingAssetsUseCase, authStateHolder)
        authStateHolder.updateSession(sampleSession())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BookingAuthGate.Authenticated, state.authGate)
        assertEquals(BookingAssetStatus.ONLINE, state.selectedStatus)
        assertEquals(listOf("online-1"), state.items.map { it.dramaId })
        assertEquals("已上线", state.items.single().statusLabel)
        assertEquals(3, state.summary.onlineCount)
        assertEquals(5, state.summary.upcomingCount)
        assertEquals(1, state.page)
        assertTrue(state.hasNextPage)
        assertTrue(state.hasLoadedOnce)
        assertFalse(state.isLoading)

        coVerify(exactly = 1) { getBookingAssetsUseCase.invoke(firstPage) }
    }

    @Test
    fun `T-11 quick status switching only consumes latest response`() = runTest {
        val onlineGate = CompletableDeferred<Unit>()
        val upcomingGate = CompletableDeferred<Unit>()
        val onlineQuery = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 1, pageSize = 20)
        val upcomingQuery = BookingAssetsQuery(status = BookingAssetStatus.UPCOMING, page = 1, pageSize = 20)

        coEvery { getBookingAssetsUseCase.invoke(onlineQuery) } coAnswers {
            onlineGate.await()
            ApiResult.Success(
                bookingPage(
                    status = BookingAssetStatus.ONLINE,
                    ids = listOf("stale-online"),
                    summary = BookingAssetSummary(onlineCount = 1, upcomingCount = 1),
                ),
            )
        }
        coEvery { getBookingAssetsUseCase.invoke(upcomingQuery) } coAnswers {
            upcomingGate.await()
            ApiResult.Success(
                bookingPage(
                    status = BookingAssetStatus.UPCOMING,
                    ids = listOf("latest-upcoming"),
                    summary = BookingAssetSummary(onlineCount = 1, upcomingCount = 7),
                ),
            )
        }

        val viewModel = BookingAssetsViewModel(getBookingAssetsUseCase, authStateHolder)
        authStateHolder.updateSession(sampleSession())
        runCurrent()

        viewModel.onStatusSelected(BookingAssetStatus.UPCOMING)
        runCurrent()

        upcomingGate.complete(Unit)
        advanceUntilIdle()
        onlineGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BookingAssetStatus.UPCOMING, state.selectedStatus)
        assertEquals(listOf("latest-upcoming"), state.items.map { it.dramaId })
        assertEquals(7, state.summary.upcomingCount)
    }

    @Test
    fun `T-11 load next page appends once and ignores duplicate triggers`() = runTest {
        val firstPage = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 1, pageSize = 20)
        val secondPage = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 2, pageSize = 20)
        val appendGate = CompletableDeferred<Unit>()

        coEvery { getBookingAssetsUseCase.invoke(firstPage) } returns ApiResult.Success(
            bookingPage(
                status = BookingAssetStatus.ONLINE,
                ids = listOf("page-1"),
                page = 1,
                totalPages = 2,
            ),
        )
        coEvery { getBookingAssetsUseCase.invoke(secondPage) } coAnswers {
            appendGate.await()
            ApiResult.Success(
                bookingPage(
                    status = BookingAssetStatus.ONLINE,
                    ids = listOf("page-2"),
                    page = 2,
                    totalPages = 2,
                ),
            )
        }

        val viewModel = BookingAssetsViewModel(getBookingAssetsUseCase, authStateHolder)
        authStateHolder.updateSession(sampleSession())
        advanceUntilIdle()

        viewModel.loadNextPageIfNeeded()
        viewModel.loadNextPageIfNeeded()
        runCurrent()

        coVerify(exactly = 1) { getBookingAssetsUseCase.invoke(secondPage) }

        appendGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("page-1", "page-2"), state.items.map { it.dramaId })
        assertEquals(2, state.page)
        assertFalse(state.hasNextPage)
        assertFalse(state.isAppending)
    }

    @Test
    fun `T-11 append unauthorized resets into expired login gate`() = runTest {
        val firstPage = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 1, pageSize = 20)
        val secondPage = BookingAssetsQuery(status = BookingAssetStatus.ONLINE, page = 2, pageSize = 20)

        coEvery { getBookingAssetsUseCase.invoke(firstPage) } returns ApiResult.Success(
            bookingPage(
                status = BookingAssetStatus.ONLINE,
                ids = listOf("page-1"),
                page = 1,
                totalPages = 2,
            ),
        )
        coEvery { getBookingAssetsUseCase.invoke(secondPage) } returns ApiResult.Error(
            code = "AUTH_UNAUTHORIZED",
            message = "请先登录",
        )

        val viewModel = BookingAssetsViewModel(getBookingAssetsUseCase, authStateHolder)
        authStateHolder.updateSession(sampleSession())
        advanceUntilIdle()

        viewModel.loadNextPageIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BookingAuthGate.Expired, state.authGate)
        assertTrue(state.showLoginGate)
        assertTrue(state.items.isEmpty())
        assertEquals(BookingAssetStatus.ONLINE, state.selectedStatus)
    }

    private fun bookingPage(
        status: BookingAssetStatus,
        ids: List<String>,
        page: Int = 1,
        pageSize: Int = 20,
        totalPages: Int = 1,
        summary: BookingAssetSummary = BookingAssetSummary(onlineCount = 0, upcomingCount = 0),
    ): BookingAssetsPage = BookingAssetsPage(
        items = ids.map { id ->
            BookingAsset(
                dramaId = id,
                title = "预约短剧$id",
                coverUrl = "https://example.com/$id.jpg",
                episodeCount = 12,
                bookedAt = "2026-07-30T03:25:00.000Z",
                availabilityStatus = status,
            )
        },
        page = page,
        pageSize = pageSize,
        total = ids.size * totalPages,
        totalPages = totalPages,
        summary = summary,
    )

    private fun sampleSession(): AuthSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-30T12:34:56Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = null,
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = false,
        ),
    )
}

private class FakeBookingAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null

    override suspend fun read(): AuthSession? = session

    override suspend fun write(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
