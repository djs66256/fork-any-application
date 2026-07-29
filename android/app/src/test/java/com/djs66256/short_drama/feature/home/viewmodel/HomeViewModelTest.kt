package com.djs66256.short_drama.feature.home.viewmodel

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInDayStatus
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.CheckInRepository
import com.djs66256.short_drama.domain.usecase.GetCheckInStatusUseCase
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import com.djs66256.short_drama.domain.usecase.SubmitCheckInUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getDramasUseCase = mockk<GetDramasUseCase>()
    private val getCheckInStatusUseCase = mockk<GetCheckInStatusUseCase>()
    private val submitCheckInUseCase = mockk<SubmitCheckInUseCase>()
    private val checkInRepository = mockk<CheckInRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-04 loadIfNeeded shows popup when check in is eligible`() = runTest {
        val status = sampleCheckInStatus()
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(status)
        coEvery { checkInRepository.getDismissedServerDate() } returns null

        val viewModel = HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertTrue(state.hasLoadedOnce)
        assertTrue(state.checkInPopup.isVisible)
        assertEquals(status.serverDate, state.checkInPopup.serverDate)
        assertEquals(status.rewardCopy, state.checkInPopup.rewardCopy)
        assertFalse(state.checkInPopup.todaySigned)
    }

    @Test
    fun `T-04 loadIfNeeded hides popup when same server date was dismissed`() = runTest {
        val status = sampleCheckInStatus(serverDate = "2026-07-30")
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(status)
        coEvery { checkInRepository.getDismissedServerDate() } returns "2026-07-30"

        val viewModel = HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
        )

        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.checkInPopup.isVisible)
        assertEquals(status.serverDate, state.checkInPopup.serverDate)
    }

    @Test
    fun `T-04 submitCheckIn updates popup and stores dismissed server date`() = runTest {
        val initialStatus = sampleCheckInStatus(todaySigned = false)
        val submittedStatus = sampleCheckInStatus(
            todaySigned = true,
            shouldShowPopup = false,
            days = listOf(
                CheckInDay(1, "第 1 天", "金币 x10", CheckInDayStatus.SIGNED),
                CheckInDay(2, "第 2 天", "金币 x20", CheckInDayStatus.SIGNED),
                CheckInDay(3, "第 3 天", "金币 x30", CheckInDayStatus.TODAY),
            ),
        )
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(initialStatus)
        coEvery { checkInRepository.getDismissedServerDate() } returns null
        coEvery { submitCheckInUseCase() } returns ApiResult.Success(submittedStatus)

        val viewModel = HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
        )
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.submitCheckIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.checkInPopup.isVisible)
        assertTrue(state.checkInPopup.todaySigned)
        assertFalse(state.checkInPopup.isSubmitting)
        assertNull(state.checkInPopup.submitErrorMessage)
        coVerify { checkInRepository.dismissForServerDate(submittedStatus.serverDate) }
    }

    @Test
    fun `T-04 submitCheckIn keeps popup visible on failure`() = runTest {
        val initialStatus = sampleCheckInStatus(todaySigned = false)
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(initialStatus)
        coEvery { checkInRepository.getDismissedServerDate() } returns null
        coEvery { submitCheckInUseCase() } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "签到失败，请重试",
        )

        val viewModel = HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
        )
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.submitCheckIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.checkInPopup.isVisible)
        assertFalse(state.checkInPopup.todaySigned)
        assertEquals("签到失败，请重试", state.checkInPopup.submitErrorMessage)
        assertFalse(state.checkInPopup.isSubmitting)
    }

    @Test
    fun `T-04 abandonCheckInPopupForCurrentSession hides visible popup`() = runTest {
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(sampleCheckInStatus())
        coEvery { checkInRepository.getDismissedServerDate() } returns null

        val viewModel = HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
        )
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.abandonCheckInPopupForCurrentSession()

        assertFalse(viewModel.uiState.value.checkInPopup.isVisible)
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

    private fun sampleCheckInStatus(
        serverDate: String = "2026-07-29",
        shouldShowPopup: Boolean = true,
        todaySigned: Boolean = false,
        days: List<CheckInDay> = listOf(
            CheckInDay(1, "第 1 天", "金币 x10", CheckInDayStatus.SIGNED),
            CheckInDay(2, "第 2 天", "金币 x20", CheckInDayStatus.SIGNED),
            CheckInDay(3, "第 3 天", "金币 x30", CheckInDayStatus.TODAY),
        ),
    ): CheckInStatus = CheckInStatus(
        serverDate = serverDate,
        shouldShowPopup = shouldShowPopup,
        todaySigned = todaySigned,
        currentStreak = 2,
        rewardCopy = "今日签到可领取第 3 天奖励",
        days = days,
    )
}
