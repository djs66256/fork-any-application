package com.djs66256.short_drama.feature.player.player

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

class PlaceholderNativePlayerAdapter : NativePlayerAdapter {
    private var currentPosition: Double = 0.0

    override fun attach(sourceUrl: String) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(positionSeconds: Double) {
        currentPosition = positionSeconds
    }

    override fun setPlaybackSpeed(speed: Float) = Unit

    override fun currentPositionSeconds(): Double = currentPosition

    override fun release() = Unit
}

private val PosterShadow = Color(0xFF050505)
private val PosterGold = Color(0xFFE8C368)
private val PosterRed = Color(0xFFFB6B58)
private val PosterOrange = Color(0xFFFFA83D)
private val PosterSkin = Color(0xFFAE7F65)
private val PosterSkinShade = Color(0xFF7F594A)
private val PosterBlueGray = Color(0xFF5C657D)
private val PosterSteel = Color(0xFF9C9A9D)
private val PosterScar = Color(0xFF7A3730)
private val PlayerBackground = Color(0xFF000000)

@Composable
fun PlaceholderPlayerHost(
    uiState: PlayerUiState,
    modifier: Modifier = Modifier,
    eventAdapter: PlayerEventAdapter = PlayerEventAdapter(),
) {
    val sourceUrl = uiState.currentEpisode?.videoUrl.orEmpty().trim()
    if (sourceUrl.isBlank()) {
        PosterFallbackHost(
            uiState = uiState,
            modifier = modifier,
        )
        return
    }

    NativeVideoPlayerHost(
        sourceUrl = sourceUrl,
        resumeProgressSeconds = uiState.resumeProgress,
        playbackSpeed = uiState.currentSpeed,
        shouldPlay = uiState.shouldAutoPlayVideo,
        eventAdapter = eventAdapter,
        modifier = modifier,
    )
}

@Composable
private fun NativeVideoPlayerHost(
    sourceUrl: String,
    resumeProgressSeconds: Double,
    playbackSpeed: PlaybackSpeed,
    shouldPlay: Boolean,
    eventAdapter: PlayerEventAdapter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestEventAdapter by rememberUpdatedState(eventAdapter)
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember(sourceUrl) { mutableStateOf(false) }
    var initialSeekApplied by remember(sourceUrl) { mutableStateOf(false) }
    val resumePositionMs = remember(sourceUrl, resumeProgressSeconds) {
        resumeProgressSeconds.coerceAtLeast(0.0).times(1000).toInt()
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBackground),
        factory = {
            VideoView(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                setOnPreparedListener { preparedPlayer ->
                    mediaPlayer = preparedPlayer
                    isPrepared = true
                    preparedPlayer.isLooping = true
                    applyPlaybackSpeed(preparedPlayer, playbackSpeed.multiplier)
                    applyInitialSeek(this, resumePositionMs, initialSeekApplied) {
                        initialSeekApplied = it
                    }
                    if (shouldPlay) {
                        start()
                        latestEventAdapter.onPlayingChanged(true)
                    } else {
                        pause()
                        latestEventAdapter.onPlayingChanged(false)
                    }
                }
                setOnCompletionListener {
                    start()
                    latestEventAdapter.onPlayingChanged(true)
                }
                setOnErrorListener { _, _, _ ->
                    latestEventAdapter.onPlaybackError(DEFAULT_PLAYBACK_ERROR_MESSAGE)
                    true
                }
                setVideoURI(Uri.parse(sourceUrl))
                tag = sourceUrl
            }.also { createdView ->
                videoView = createdView
            }
        },
        update = { updatedView ->
            videoView = updatedView
            if (updatedView.tag != sourceUrl) {
                isPrepared = false
                initialSeekApplied = false
                updatedView.stopPlayback()
                updatedView.setVideoURI(Uri.parse(sourceUrl))
                updatedView.tag = sourceUrl
            }
            if (isPrepared) {
                applyPlaybackSpeed(mediaPlayer, playbackSpeed.multiplier)
                applyInitialSeek(updatedView, resumePositionMs, initialSeekApplied) {
                    initialSeekApplied = it
                }
                if (shouldPlay && !updatedView.isPlaying) {
                    updatedView.start()
                    latestEventAdapter.onPlayingChanged(true)
                } else if (!shouldPlay && updatedView.isPlaying) {
                    updatedView.pause()
                    latestEventAdapter.onPlayingChanged(false)
                }
            }
        },
    )

    LaunchedEffect(videoView, sourceUrl) {
        while (true) {
            delay(POSITION_POLL_INTERVAL_MS)
            val currentPositionSeconds = videoView?.currentPosition?.div(1000.0) ?: continue
            latestEventAdapter.onPositionChanged(currentPositionSeconds)
        }
    }

    DisposableEffect(sourceUrl) {
        onDispose {
            videoView?.stopPlayback()
            mediaPlayer = null
            videoView = null
        }
    }
}

private fun applyInitialSeek(
    videoView: VideoView,
    resumePositionMs: Int,
    initialSeekApplied: Boolean,
    onApplied: (Boolean) -> Unit,
) {
    if (initialSeekApplied) {
        return
    }
    if (resumePositionMs > 0) {
        videoView.seekTo(resumePositionMs)
    }
    onApplied(true)
}

private fun applyPlaybackSpeed(
    mediaPlayer: MediaPlayer?,
    speed: Float,
) {
    mediaPlayer ?: return
    runCatching {
        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
    }
}

private val PlayerUiState.shouldAutoPlayVideo: Boolean
    get() = when (screenState) {
        PlayerScreenState.READY,
        PlayerScreenState.PLAYING,
        PlayerScreenState.SWITCHING_EPISODE,
        -> true

        PlayerScreenState.IDLE,
        PlayerScreenState.BOOTSTRAPPING,
        PlayerScreenState.PAUSED,
        PlayerScreenState.NO_RESOURCE,
        PlayerScreenState.ERROR,
        -> false
    }

@Composable
private fun PosterFallbackHost(
    uiState: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF07120F), Color(0xFF10241A), Color(0xFF0A1110)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(262.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x44000000), Color(0x78000000)),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(182.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 88.dp)
                    .width(252.dp)
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PosterSteel, PosterBlueGray, Color(0xFF3E3D43), Color(0xFF272329)),
                        ),
                        RoundedCornerShape(topStart = 112.dp, topEnd = 84.dp, bottomStart = 24.dp, bottomEnd = 18.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 34.dp)
                    .width(78.dp)
                    .height(136.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF6C7378), Color(0xFF4B4D53), Color(0xFF272328)),
                        ),
                        RoundedCornerShape(topStart = 48.dp, topEnd = 26.dp, bottomStart = 12.dp, bottomEnd = 10.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 102.dp, bottom = 18.dp)
                    .width(232.dp)
                    .height(124.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2F241E), Color(0xFF5A4337), Color(0xFF2E231D)),
                        ),
                        RoundedCornerShape(10.dp),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
                .width(420.dp)
                .height(420.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PosterBlueGray, Color(0xFF88818A), Color(0xFF56474B), Color(0xFF1A161C)),
                    ),
                    RoundedCornerShape(topStart = 160.dp, topEnd = 160.dp, bottomStart = 76.dp, bottomEnd = 76.dp),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 172.dp)
                .width(214.dp)
                .height(246.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PosterSkin, Color(0xFF9A6F5D), PosterSkinShade, Color(0xFF553D37)),
                    ),
                    RoundedCornerShape(topStart = 116.dp, topEnd = 116.dp, bottomStart = 88.dp, bottomEnd = 88.dp),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 372.dp)
                .width(184.dp)
                .height(84.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF7B7C85), Color(0xFF55565F), Color(0xFF3B3B43)),
                    ),
                    RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 205.dp)
                .width(122.dp)
                .height(20.dp)
                .background(Color(0xFF2D1917), RoundedCornerShape(12.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 242.dp, start = 80.dp)
                .width(18.dp)
                .height(42.dp)
                .background(Color(0xFF2B1817), RoundedCornerShape(10.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 270.dp)
                .width(11.dp)
                .height(32.dp)
                .background(PosterScar, RoundedCornerShape(6.dp)),
        )

        FacialFeature(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 292.dp, end = 64.dp),
            width = 48.dp,
            height = 10.dp,
            color = PosterShadow,
        )
        FacialFeature(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 286.dp, start = 64.dp),
            width = 48.dp,
            height = 10.dp,
            color = PosterShadow,
        )
        FacialFeature(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 312.dp, end = 66.dp),
            width = 60.dp,
            height = 7.dp,
            color = Color(0xFF6A544E),
        )
        FacialFeature(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 306.dp, start = 66.dp),
            width = 60.dp,
            height = 7.dp,
            color = Color(0xFF6A544E),
        )
        FacialFeature(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 216.dp),
            width = 86.dp,
            height = 10.dp,
            color = Color(0xFF39201C),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 38.dp, bottom = 22.dp)
                .size(106.dp)
                .background(PosterGold, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(88.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PosterOrange, PosterRed),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .width(52.dp)
                    .height(24.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFC56C), Color(0xFFFF9F33), Color(0xFFFFD88B)),
                        ),
                        RoundedCornerShape(14.dp),
                    ),
            )
            androidx.compose.material3.Text(
                text = "12879",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (uiState.resumeProgress > 0.0 || uiState.currentSpeed != PlaybackSpeed.X1_0) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 110.dp)
                    .background(Color(0x80141414), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "恢复点 ${uiState.resumeProgress.toInt()}s",
                    color = Color.White,
                    fontSize = 12.sp,
                )
                androidx.compose.material3.Text(
                    text = "倍速 ${uiState.currentSpeed.label}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun FacialFeature(
    modifier: Modifier,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(color, RoundedCornerShape(percent = 50)),
    )
}

private const val POSITION_POLL_INTERVAL_MS = 500L
private const val DEFAULT_PLAYBACK_ERROR_MESSAGE = "视频播放失败，请重试"
