package com.djs66256.short_drama.feature.home.viewmodel

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInDayStatus
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.repository.CheckInRepository
import com.djs66256.short_drama.domain.usecase.GetCheckInStatusUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaEpisodesUseCase
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import com.djs66256.short_drama.domain.usecase.GetPlaybackProgressUseCase
import com.djs66256.short_drama.domain.usecase.StartPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.StopPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.SubmitCheckInUseCase
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private val getPlaybackProgressUseCase = mockk<GetPlaybackProgressUseCase>()
    private val getDramaEpisodesUseCase = mockk<GetDramaEpisodesUseCase>()
    private val startPlaybackUseCase = mockk<StartPlaybackUseCase>()
    private val stopPlaybackUseCase = mockk<StopPlaybackUseCase>(relaxed = true)

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

        val viewModel = createViewModel()

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

        val viewModel = createViewModel()

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

        val viewModel = createViewModel()
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

        val viewModel = createViewModel()
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

        val viewModel = createViewModel()
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.abandonCheckInPopupForCurrentSession()

        assertFalse(viewModel.uiState.value.checkInPopup.isVisible)
    }

    @Test
    fun `featured drama popup shows for three seconds after home content is presented`() = runTest {
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(
            sampleCheckInStatus(shouldShowPopup = false),
        )
        coEvery { checkInRepository.getDismissedServerDate() } returns null

        val viewModel = createViewModel()
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.onHomeContentPresented(hasBlockingModal = false)

        assertTrue(viewModel.uiState.value.featuredDramaPopup.isVisible)

        advanceTimeBy(2_999L)
        assertTrue(viewModel.uiState.value.featuredDramaPopup.isVisible)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.featuredDramaPopup.isVisible)
    }

    @Test
    fun `featured drama popup is skipped when content presentation is blocked and only shows once`() = runTest {
        coEvery { getDramasUseCase(page = 1, pageSize = 10) } returns ApiResult.Success(listOf(sampleDrama()))
        coEvery { getCheckInStatusUseCase() } returns ApiResult.Success(
            sampleCheckInStatus(shouldShowPopup = false),
        )
        coEvery { checkInRepository.getDismissedServerDate() } returns null

        val viewModel = createViewModel()
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        viewModel.onHomeContentPresented(hasBlockingModal = true)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.featuredDramaPopup.isVisible)

        viewModel.onHomeContentPresented(hasBlockingModal = false)
        assertTrue(viewModel.uiState.value.featuredDramaPopup.isVisible)

        advanceTimeBy(3_000L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.featuredDramaPopup.isVisible)

        viewModel.onHomeContentPresented(hasBlockingModal = false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.featuredDramaPopup.isVisible)
    }

    @Test
    fun `home feed visible drama change bootstraps current player episode`() = runTest {
        coEvery { getPlaybackProgressUseCase("drama-1") } returns ApiResult.Success(
            PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
        )
        coEvery { getDramaEpisodesUseCase("drama-1") } returns ApiResult.Success(
            DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(playableEpisode(1)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-1", 0.0))
        } returns ApiResult.Success(startResult("episode-1", 0.0))

        val viewModel = createViewModel()
        viewModel.onVisibleDramaChanged("drama-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("drama-1", state.activeDramaId)
        assertEquals(PlayerScreenState.PLAYING, state.activePlayerUiState.screenState)
        assertEquals("episode-1", state.activePlayerUiState.currentEpisode?.id)
        assertEquals(0.0, state.activePlayerUiState.resumeProgress, 0.0)
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getDramasUseCase = getDramasUseCase,
            getCheckInStatusUseCase = getCheckInStatusUseCase,
            submitCheckInUseCase = submitCheckInUseCase,
            checkInRepository = checkInRepository,
            getPlaybackProgressUseCase = getPlaybackProgressUseCase,
            getDramaEpisodesUseCase = getDramaEpisodesUseCase,
            startPlaybackUseCase = startPlaybackUseCase,
            stopPlaybackUseCase = stopPlaybackUseCase,
        )
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

    private fun playableEpisode(number: Int): Episode = Episode(
        id = "episode-$number",
        dramaId = "drama-1",
        title = "第 $number 集",
        episodeNumber = number,
        videoUrl = "https://example.com/$number.mp4",
        duration = 100,
        thumbnailUrl = "https://example.com/$number.jpg",
        description = "第 $number 集简介",
        createdAt = "2026-07-26T00:00:00Z",
        updatedAt = "2026-07-26T00:00:00Z",
    )

    private fun startResult(episodeId: String, progress: Double): StartPlaybackResult = StartPlaybackResult(
        dramaId = "drama-1",
        episodeId = episodeId,
        acceptedProgress = progress,
        playbackSessionId = "session-123",
        startedAt = "2026-07-26T00:00:00Z",
    )
}
