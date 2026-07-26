package com.djs66256.short_drama.feature.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.usecase.GetDramaEpisodesUseCase
import com.djs66256.short_drama.domain.usecase.GetPlaybackProgressUseCase
import com.djs66256.short_drama.domain.usecase.StartPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.StopPlaybackUseCase
import com.djs66256.short_drama.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val getDramaEpisodesUseCase: GetDramaEpisodesUseCase,
    private val startPlaybackUseCase: StartPlaybackUseCase,
    private val stopPlaybackUseCase: StopPlaybackUseCase,
) : ViewModel() {

    val dramaId: String =
        savedStateHandle.get<String>(AppDestination.Arg.VIDEO_ID)
            ?: savedStateHandle.get<String>(AppDestination.Arg.DRAMA_ID)
            ?: savedStateHandle.get<String>(AppDestination.Arg.ID)
            ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState(dramaId = dramaId))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null
    private var stopReportJob: Job? = null
    private var currentPlaybackPositionSeconds: Double = 0.0

    fun loadIfNeeded() {
        if (uiState.value.hasLoadedOnce || requestJob?.isActive == true) {
            return
        }
        bootstrap()
    }

    fun retry() {
        if (requestJob?.isActive == true) {
            return
        }
        bootstrap(forceReload = true)
    }

    fun toggleEpisodeSheet() {
        _uiState.update { state ->
            state.copy(isEpisodeSheetVisible = !state.isEpisodeSheetVisible)
        }
    }

    fun toggleSpeedSheet() {
        _uiState.update { state ->
            state.copy(isSpeedSheetVisible = !state.isSpeedSheetVisible)
        }
    }

    fun selectSpeed(speed: PlaybackSpeed) {
        _uiState.update { state ->
            state.copy(
                currentSpeed = speed,
                isSpeedSheetVisible = false,
            )
        }
    }

    fun toggleLike() {
        _uiState.update { state ->
            state.copy(interactionState = state.interactionState.copy(liked = !state.interactionState.liked))
        }
    }

    fun toggleFavorite() {
        _uiState.update { state ->
            state.copy(interactionState = state.interactionState.copy(favorited = !state.interactionState.favorited))
        }
    }

    fun onPlaybackPositionChanged(positionSeconds: Double) {
        currentPlaybackPositionSeconds = positionSeconds.coerceAtLeast(0.0)
    }

    fun onPlaybackError(message: String) {
        _uiState.update { state ->
            state.copy(
                screenState = PlayerScreenState.ERROR,
                errorMessage = message.ifBlank { DEFAULT_ERROR_MESSAGE },
            )
        }
    }

    fun onBackgrounded() {
        _uiState.update { state ->
            if (!state.canRenderPlayerChrome) {
                state
            } else {
                state.copy(screenState = PlayerScreenState.PAUSED)
            }
        }
        reportStopBestEffort()
    }

    fun onScreenDisposed() {
        reportStopBestEffort()
    }

    fun switchEpisode(targetEpisode: Episode) {
        val currentState = uiState.value
        val previousEpisode = currentState.currentEpisode
        if (requestJob?.isActive == true || targetEpisode.id == previousEpisode?.id) {
            _uiState.update { state -> state.copy(isEpisodeSheetVisible = false) }
            return
        }
        if (targetEpisode.videoUrl.isBlank()) {
            return
        }

        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    screenState = PlayerScreenState.SWITCHING_EPISODE,
                    currentEpisode = targetEpisode,
                    resumeProgress = 0.0,
                    errorMessage = null,
                    isEpisodeSheetVisible = false,
                )
            }

            stopEpisodeBestEffort(previousEpisode)
            currentPlaybackPositionSeconds = 0.0

            when (
                val startResult = startPlaybackUseCase(
                    StartPlaybackParams(
                        dramaId = dramaId,
                        episodeId = targetEpisode.id,
                        progress = 0.0,
                    ),
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            screenState = PlayerScreenState.PLAYING,
                            currentEpisode = targetEpisode,
                            resumeProgress = 0.0,
                            errorMessage = null,
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            screenState = PlayerScreenState.ERROR,
                            errorMessage = startResult.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            screenState = PlayerScreenState.ERROR,
                            errorMessage = DEFAULT_ERROR_MESSAGE,
                        )
                    }
                }
            }
        }
    }

    private fun bootstrap(forceReload: Boolean = false) {
        if (dramaId.isBlank()) {
            _uiState.value = PlayerUiState(
                dramaId = dramaId,
                screenState = PlayerScreenState.ERROR,
                errorMessage = INVALID_DRAMA_ID_MESSAGE,
                hasLoadedOnce = true,
            )
            return
        }

        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    screenState = PlayerScreenState.BOOTSTRAPPING,
                    errorMessage = null,
                    isEpisodeSheetVisible = false,
                    isSpeedSheetVisible = false,
                    hasLoadedOnce = if (forceReload) false else state.hasLoadedOnce,
                )
            }

            try {
                val progressResult = getPlaybackProgressUseCase(dramaId)
                if (progressResult !is ApiResult.Success) {
                    handleBootstrapFailure(progressResult)
                    return@launch
                }

                val episodesResult = getDramaEpisodesUseCase(dramaId)
                if (episodesResult !is ApiResult.Success) {
                    handleBootstrapFailure(episodesResult)
                    return@launch
                }

                val progress = progressResult.data
                val episodeList = episodesResult.data
                val targetEpisode = resolveTargetEpisode(progress = progress, episodes = episodeList.items)
                if (targetEpisode == null) {
                    _uiState.update { state ->
                        state.copy(
                            episodes = episodeList.items,
                            currentEpisode = null,
                            resumeProgress = 0.0,
                            screenState = PlayerScreenState.NO_RESOURCE,
                            seriesStatus = episodeList.seriesStatus,
                            errorMessage = NO_RESOURCE_MESSAGE,
                            hasLoadedOnce = true,
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
                                episodes = episodeList.items,
                                currentEpisode = targetEpisode,
                                resumeProgress = startProgress,
                                screenState = PlayerScreenState.PLAYING,
                                seriesStatus = episodeList.seriesStatus,
                                errorMessage = null,
                                hasLoadedOnce = true,
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                episodes = episodeList.items,
                                currentEpisode = targetEpisode,
                                resumeProgress = startProgress,
                                screenState = PlayerScreenState.ERROR,
                                seriesStatus = episodeList.seriesStatus,
                                errorMessage = startResult.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                                hasLoadedOnce = true,
                            )
                        }
                    }
                    is ApiResult.Exception -> {
                        _uiState.update { state ->
                            state.copy(
                                episodes = episodeList.items,
                                currentEpisode = targetEpisode,
                                resumeProgress = startProgress,
                                screenState = PlayerScreenState.ERROR,
                                seriesStatus = episodeList.seriesStatus,
                                errorMessage = DEFAULT_ERROR_MESSAGE,
                                hasLoadedOnce = true,
                            )
                        }
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        screenState = PlayerScreenState.ERROR,
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoadedOnce = true,
                    )
                }
            }
        }
    }

    private fun handleBootstrapFailure(result: ApiResult<*>) {
        when (result) {
            is ApiResult.Error -> {
                _uiState.update { state ->
                    state.copy(
                        screenState = PlayerScreenState.ERROR,
                        errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        hasLoadedOnce = true,
                    )
                }
            }
            is ApiResult.Exception -> {
                _uiState.update { state ->
                    state.copy(
                        screenState = PlayerScreenState.ERROR,
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoadedOnce = true,
                    )
                }
            }
            is ApiResult.Success -> Unit
        }
    }

    private fun resolveTargetEpisode(
        progress: PlaybackProgress,
        episodes: List<Episode>,
    ): Episode? {
        val playableEpisodes = episodes.filter { it.videoUrl.isNotBlank() }.sortedBy(Episode::episodeNumber)
        if (playableEpisodes.isEmpty()) {
            return null
        }
        val recovered = if (progress.hasHistory) {
            playableEpisodes.firstOrNull { it.id == progress.episodeId }
        } else {
            null
        }
        return recovered ?: playableEpisodes.first()
    }

    private fun reportStopBestEffort() {
        val episode = uiState.value.currentEpisode ?: return
        stopReportJob?.cancel()
        stopReportJob = viewModelScope.launch {
            stopEpisodeBestEffort(episode)
        }
    }

    private suspend fun stopEpisodeBestEffort(episode: Episode?) {
        episode ?: return
        val duration = episode.duration.toDouble().coerceAtLeast(1.0)
        val params = StopPlaybackParams(
            dramaId = dramaId,
            episodeId = episode.id,
            progress = currentPlaybackPositionSeconds.coerceIn(0.0, duration),
            duration = duration,
        )

        _uiState.update { current -> current.copy(isReportingStop = true) }
        try {
            stopPlaybackUseCase(params)
        } catch (_: Throwable) {
            // stop 上报 best-effort，不阻塞 UI 和导航
        } finally {
            _uiState.update { current -> current.copy(isReportingStop = false) }
        }
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val NO_RESOURCE_MESSAGE = "暂无可播放内容"
        const val INVALID_DRAMA_ID_MESSAGE = "页面参数无效"
    }
}
