package com.djs66256.short_drama.feature.player.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState

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

@Composable
fun PlaceholderPlayerHost(
    uiState: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "播放器占位宿主",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = buildPlaceholderSummary(uiState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "未引入 androidx.media3；真实视频播放待用户授权新增依赖。",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private fun buildPlaceholderSummary(uiState: PlayerUiState): String {
    val episodeText = uiState.currentEpisode?.let { "第 ${it.episodeNumber} 集 · ${it.title}" } ?: "未选中剧集"
    return listOf(
        "dramaId=${uiState.dramaId.ifBlank { "-" }}",
        episodeText,
        "恢复点 ${uiState.resumeProgress.formatSeconds()}",
        "倍速 ${uiState.currentSpeed.label}",
    ).joinToString("\n")
}

private fun Double.formatSeconds(): String = String.format("%.0fs", this)
