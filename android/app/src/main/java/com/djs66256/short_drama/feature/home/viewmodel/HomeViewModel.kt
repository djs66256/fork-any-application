package com.djs66256.short_drama.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.repository.CheckInRepository
import com.djs66256.short_drama.domain.usecase.GetCheckInStatusUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaEpisodesUseCase
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import com.djs66256.short_drama.domain.usecase.GetPlaybackProgressUseCase
import com.djs66256.short_drama.domain.usecase.StartPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.StopPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.SubmitCheckInUseCase
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInPopupUiState(
    val isVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val serverDate: String? = null,
    val todaySigned: Boolean = false,
    val currentStreak: Int = 0,
    val rewardCopy: String = "",
    val days: List<CheckInDay> = emptyList(),
    val submitErrorMessage: String? = null,
)

data class FeaturedDramaPopupUiState(
    val isVisible: Boolean = false,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<Drama> = emptyList(),
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
    val activeDramaId: String? = null,
    val activePlayerUiState: PlayerUiState = PlayerUiState(),
    val featuredDramaPopup: FeaturedDramaPopupUiState = FeaturedDramaPopupUiState(),
    val checkInPopup: CheckInPopupUiState = CheckInPopupUiState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDramasUseCase: GetDramasUseCase,
    private val getCheckInStatusUseCase: GetCheckInStatusUseCase,
    private val submitCheckInUseCase: SubmitCheckInUseCase,
    private val checkInRepository: CheckInRepository,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val getDramaEpisodesUseCase: GetDramaEpisodesUseCase,
    private val startPlaybackUseCase: StartPlaybackUseCase,
    private val stopPlaybackUseCase: StopPlaybackUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var requestInFlight = false
    private var checkInPopupAbandoned = false
    private var hasShownFeaturedDramaPopupForSession = false
    private var feedPlaybackJob: Job? = null
    private var stopReportJob: Job? = null
    private var featuredDramaPopupJob: Job? = null
    private var currentPlaybackPositionSeconds: Double = 0.0

    fun loadIfNeeded() {
        if (requestInFlight || uiState.value.hasLoadedOnce) {
            return
        }
        loadDramas(isRetry = false)
    }

    fun retry() {
        val state = uiState.value
        if (requestInFlight || state.errorMessage == null) {
            return
        }
        loadDramas(isRetry = true)
    }

    fun onVisibleDramaChanged(dramaId: String) {
        if (dramaId.isBlank()) {
            return
        }
        val state = uiState.value
        if (
            state.activeDramaId == dramaId &&
            state.activePlayerUiState.hasLoadedOnce &&
            feedPlaybackJob?.isActive != true
        ) {
            onForegrounded()
            return
        }
        bootstrapFeedPlayback(dramaId)
    }

    fun onHomeContentPresented(hasBlockingModal: Boolean) {
        val state = uiState.value
        val hasContent = state.items.isNotEmpty() && !state.isLoading && state.errorMessage == null
        if (
            !hasContent ||
            hasBlockingModal ||
            hasShownFeaturedDramaPopupForSession ||
            state.featuredDramaPopup.isVisible
        ) {
            return
        }

        hasShownFeaturedDramaPopupForSession = true
        featuredDramaPopupJob?.cancel()
        _uiState.update { currentState ->
            currentState.copy(
                featuredDramaPopup = currentState.featuredDramaPopup.copy(isVisible = true),
            )
        }
        featuredDramaPopupJob = viewModelScope.launch {
            delay(FEATURED_DRAMA_POPUP_AUTO_HIDE_MILLIS)
            _uiState.update { currentState ->
                currentState.copy(
                    featuredDramaPopup = currentState.featuredDramaPopup.copy(isVisible = false),
                )
            }
        }
    }

    fun onFeedPlaybackPositionChanged(positionSeconds: Double) {
        currentPlaybackPositionSeconds = positionSeconds.coerceAtLeast(0.0)
    }

    fun onFeedPlaybackError(message: String) {
        if (uiState.value.activeDramaId.isNullOrBlank()) {
            return
        }
        _uiState.update { state ->
            state.copy(
                activePlayerUiState = state.activePlayerUiState.copy(
                    screenState = PlayerScreenState.ERROR,
                    errorMessage = message.ifBlank { DEFAULT_PLAYBACK_ERROR_MESSAGE },
                ),
            )
        }
    }

    fun onForegrounded() {
        _uiState.update { state ->
            if (
                state.activeDramaId.isNullOrBlank() ||
                !state.activePlayerUiState.canRenderPlayerChrome
            ) {
                state
            } else {
                state.copy(
                    activePlayerUiState = state.activePlayerUiState.copy(
                        screenState = PlayerScreenState.PLAYING,
                    ),
                )
            }
        }
    }

    fun onBackgrounded() {
        _uiState.update { state ->
            if (
                state.activeDramaId.isNullOrBlank() ||
                !state.activePlayerUiState.canRenderPlayerChrome
            ) {
                state
            } else {
                state.copy(
                    activePlayerUiState = state.activePlayerUiState.copy(
                        screenState = PlayerScreenState.PAUSED,
                    ),
                )
            }
        }
        reportStopBestEffort()
    }

    fun onScreenDisposed() {
        feedPlaybackJob?.cancel()
        featuredDramaPopupJob?.cancel()
        reportStopBestEffort()
    }

    fun submitCheckIn() {
        val popupState = uiState.value.checkInPopup
        if (!popupState.isVisible || popupState.isSubmitting || popupState.todaySigned) {
            return
        }

        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(
                    isSubmitting = true,
                    submitErrorMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            when (val result = submitCheckInUseCase()) {
                is ApiResult.Success -> {
                    checkInRepository.dismissForServerDate(result.data.serverDate)
                    applyCheckInStatus(result.data, forceVisible = true)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            checkInPopup = state.checkInPopup.copy(
                                isSubmitting = false,
                                submitErrorMessage = result.message.ifBlank { DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE },
                            ),
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            checkInPopup = state.checkInPopup.copy(
                                isSubmitting = false,
                                submitErrorMessage = DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun dismissCheckInPopup() {
        val serverDate = uiState.value.checkInPopup.serverDate ?: return
        checkInPopupAbandoned = true
        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(isVisible = false),
            )
        }
        viewModelScope.launch {
            checkInRepository.dismissForServerDate(serverDate)
        }
    }

    fun abandonCheckInPopupForCurrentSession() {
        checkInPopupAbandoned = true
        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(isVisible = false),
            )
        }
    }

    private fun loadDramas(isRetry: Boolean) {
        requestInFlight = true
        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null,
                isRetrying = isRetry,
            )
        }

        viewModelScope.launch {
            try {
                val result = getDramasUseCase(page = FIRST_PAGE, pageSize = FEED_PAGE_SIZE)
                _uiState.value = when (result) {
                    is ApiResult.Success -> HomeUiState(
                        isLoading = false,
                        items = result.data,
                        errorMessage = null,
                        hasLoadedOnce = true,
                        isRetrying = false,
                        activeDramaId = _uiState.value.activeDramaId,
                        activePlayerUiState = _uiState.value.activePlayerUiState,
                        featuredDramaPopup = _uiState.value.featuredDramaPopup,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                    is ApiResult.Error -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        hasLoadedOnce = true,
                        isRetrying = false,
                        activeDramaId = _uiState.value.activeDramaId,
                        activePlayerUiState = _uiState.value.activePlayerUiState,
                        featuredDramaPopup = _uiState.value.featuredDramaPopup,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                    is ApiResult.Exception -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoadedOnce = true,
                        isRetrying = false,
                        activeDramaId = _uiState.value.activeDramaId,
                        activePlayerUiState = _uiState.value.activePlayerUiState,
                        featuredDramaPopup = _uiState.value.featuredDramaPopup,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                }
                loadCheckInStatusIfNeeded()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    items = emptyList(),
                    errorMessage = DEFAULT_ERROR_MESSAGE,
                    hasLoadedOnce = true,
                    isRetrying = false,
                    activeDramaId = _uiState.value.activeDramaId,
                    activePlayerUiState = _uiState.value.activePlayerUiState,
                    featuredDramaPopup = _uiState.value.featuredDramaPopup,
                    checkInPopup = _uiState.value.checkInPopup,
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private fun bootstrapFeedPlayback(dramaId: String) {
        val previousDramaId = uiState.value.activeDramaId
        val previousEpisode = uiState.value.activePlayerUiState.currentEpisode
        feedPlaybackJob?.cancel()
        feedPlaybackJob = viewModelScope.launch {
            if (!previousDramaId.isNullOrBlank() && previousDramaId != dramaId) {
                stopEpisodeBestEffort(previousDramaId, previousEpisode)
            }

            _uiState.update { state ->
                state.copy(
                    activeDramaId = dramaId,
                    activePlayerUiState = PlayerUiState(
                        dramaId = dramaId,
                        screenState = PlayerScreenState.BOOTSTRAPPING,
                    ),
                )
            }

            try {
                val progressResult = getPlaybackProgressUseCase(dramaId)
                if (progressResult !is ApiResult.Success) {
                    handleFeedBootstrapFailure(dramaId, progressResult)
                    return@launch
                }

                val episodesResult = getDramaEpisodesUseCase(dramaId)
                if (episodesResult !is ApiResult.Success) {
                    handleFeedBootstrapFailure(dramaId, episodesResult)
                    return@launch
                }

                val progress = progressResult.data
                val episodeList = episodesResult.data
                val targetEpisode = resolveTargetEpisode(progress = progress, episodes = episodeList)
                if (targetEpisode == null) {
                    _uiState.update { state ->
                        state.copy(
                            activeDramaId = dramaId,
                            activePlayerUiState = PlayerUiState(
                                dramaId = dramaId,
                                episodes = episodeList.items,
                                currentEpisode = null,
                                resumeProgress = 0.0,
                                screenState = PlayerScreenState.NO_RESOURCE,
                                seriesStatus = episodeList.seriesStatus,
                                errorMessage = NO_RESOURCE_MESSAGE,
                                hasLoadedOnce = true,
                            ),
                        )
                    }
                    return@launch
                }

                val startProgress = if (progress.hasHistory && progress.episodeId == targetEpisode.id) {
                    progress.startTime.coerceAtLeast(0.0)
                } else {
                    0.0
                }
                currentPlaybackPositionSeconds = startProgress

                when (
                    val startResult = startPlaybackUseCase(
                        StartPlaybackParams(
                            dramaId = dramaId,
                            episodeId = targetEpisode.id,
                            progress = startProgress,
                        ),
                    )
                ) {
                    is ApiResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                activeDramaId = dramaId,
                                activePlayerUiState = PlayerUiState(
                                    dramaId = dramaId,
                                    episodes = episodeList.items,
                                    currentEpisode = targetEpisode,
                                    resumeProgress = startProgress,
                                    screenState = PlayerScreenState.PLAYING,
                                    seriesStatus = episodeList.seriesStatus,
                                    errorMessage = null,
                                    hasLoadedOnce = true,
                                ),
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                activeDramaId = dramaId,
                                activePlayerUiState = PlayerUiState(
                                    dramaId = dramaId,
                                    episodes = episodeList.items,
                                    currentEpisode = targetEpisode,
                                    resumeProgress = startProgress,
                                    screenState = PlayerScreenState.ERROR,
                                    seriesStatus = episodeList.seriesStatus,
                                    errorMessage = startResult.message.ifBlank { DEFAULT_PLAYBACK_ERROR_MESSAGE },
                                    hasLoadedOnce = true,
                                ),
                            )
                        }
                    }
                    is ApiResult.Exception -> {
                        _uiState.update { state ->
                            state.copy(
                                activeDramaId = dramaId,
                                activePlayerUiState = PlayerUiState(
                                    dramaId = dramaId,
                                    episodes = episodeList.items,
                                    currentEpisode = targetEpisode,
                                    resumeProgress = startProgress,
                                    screenState = PlayerScreenState.ERROR,
                                    seriesStatus = episodeList.seriesStatus,
                                    errorMessage = DEFAULT_PLAYBACK_ERROR_MESSAGE,
                                    hasLoadedOnce = true,
                                ),
                            )
                        }
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        activeDramaId = dramaId,
                        activePlayerUiState = PlayerUiState(
                            dramaId = dramaId,
                            screenState = PlayerScreenState.ERROR,
                            errorMessage = DEFAULT_PLAYBACK_ERROR_MESSAGE,
                            hasLoadedOnce = true,
                        ),
                    )
                }
            }
        }
    }

    private fun handleFeedBootstrapFailure(dramaId: String, result: ApiResult<*>) {
        when (result) {
            is ApiResult.Error -> {
                _uiState.update { state ->
                    state.copy(
                        activeDramaId = dramaId,
                        activePlayerUiState = PlayerUiState(
                            dramaId = dramaId,
                            screenState = PlayerScreenState.ERROR,
                            errorMessage = result.message.ifBlank { DEFAULT_PLAYBACK_ERROR_MESSAGE },
                            hasLoadedOnce = true,
                        ),
                    )
                }
            }
            is ApiResult.Exception -> {
                _uiState.update { state ->
                    state.copy(
                        activeDramaId = dramaId,
                        activePlayerUiState = PlayerUiState(
                            dramaId = dramaId,
                            screenState = PlayerScreenState.ERROR,
                            errorMessage = DEFAULT_PLAYBACK_ERROR_MESSAGE,
                            hasLoadedOnce = true,
                        ),
                    )
                }
            }
            is ApiResult.Success -> Unit
        }
    }

    private fun resolveTargetEpisode(
        progress: PlaybackProgress,
        episodes: DramaEpisodeList,
    ): Episode? {
        val playableEpisodes = episodes.items
            .filter { it.videoUrl.isNotBlank() }
            .sortedBy(Episode::episodeNumber)
        if (playableEpisodes.isEmpty()) {
            return null
        }
        val recoveredEpisode = if (progress.hasHistory) {
            playableEpisodes.firstOrNull { it.id == progress.episodeId }
        } else {
            null
        }
        return recoveredEpisode ?: playableEpisodes.first()
    }

    private fun reportStopBestEffort() {
        val dramaId = uiState.value.activeDramaId ?: return
        val episode = uiState.value.activePlayerUiState.currentEpisode ?: return
        stopReportJob?.cancel()
        stopReportJob = viewModelScope.launch {
            stopEpisodeBestEffort(dramaId, episode)
        }
    }

    private suspend fun stopEpisodeBestEffort(
        dramaId: String,
        episode: Episode?,
    ) {
        episode ?: return
        val duration = episode.duration.toDouble().coerceAtLeast(1.0)
        val params = StopPlaybackParams(
            dramaId = dramaId,
            episodeId = episode.id,
            progress = currentPlaybackPositionSeconds.coerceIn(0.0, duration),
            duration = duration,
        )

        _uiState.update { state ->
            state.copy(
                activePlayerUiState = state.activePlayerUiState.copy(isReportingStop = true),
            )
        }
        try {
            stopPlaybackUseCase(params)
        } catch (_: Throwable) {
            // best-effort
        } finally {
            _uiState.update { state ->
                state.copy(
                    activePlayerUiState = state.activePlayerUiState.copy(isReportingStop = false),
                )
            }
        }
    }

    private suspend fun loadCheckInStatusIfNeeded() {
        if (checkInPopupAbandoned) {
            return
        }

        when (val result = getCheckInStatusUseCase()) {
            is ApiResult.Success -> applyCheckInStatus(result.data)
            is ApiResult.Error -> Unit
            is ApiResult.Exception -> Unit
        }
    }

    private suspend fun applyCheckInStatus(
        status: CheckInStatus,
        forceVisible: Boolean = false,
    ) {
        val dismissedServerDate = checkInRepository.getDismissedServerDate()
        val shouldShow = forceVisible || (
            !checkInPopupAbandoned &&
                status.shouldShowPopup &&
                dismissedServerDate != status.serverDate
        )

        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(
                    isVisible = shouldShow,
                    isSubmitting = false,
                    serverDate = status.serverDate,
                    todaySigned = status.todaySigned,
                    currentStreak = status.currentStreak,
                    rewardCopy = status.rewardCopy,
                    days = status.days,
                    submitErrorMessage = null,
                ),
            )
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val FEED_PAGE_SIZE = 10
        const val FEATURED_DRAMA_POPUP_AUTO_HIDE_MILLIS = 3_000L
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE = "签到失败，请重试"
        const val DEFAULT_PLAYBACK_ERROR_MESSAGE = "视频加载失败，请重试"
        const val NO_RESOURCE_MESSAGE = "暂无可播放内容"
    }
}
