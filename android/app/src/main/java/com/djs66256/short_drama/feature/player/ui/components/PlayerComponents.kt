package com.djs66256.short_drama.feature.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerInteractionState

@Composable
fun PlayerTopBar(
    title: String,
    currentSpeedLabel: String,
    onBack: () -> Unit,
    onToggleSpeedSheet: () -> Unit,
    onToggleMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onBack) {
            Text("返回")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onToggleSpeedSheet) {
                Text(currentSpeedLabel)
            }
            OutlinedButton(onClick = onToggleMore) {
                Text("更多")
            }
        }
    }
}

@Composable
fun PlayerRightActionBar(
    interactionState: PlayerInteractionState,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = interactionState.liked,
            onClick = onToggleLike,
            label = { Text(if (interactionState.liked) "已点赞" else "点赞") },
        )
        FilterChip(
            selected = interactionState.favorited,
            onClick = onToggleFavorite,
            label = { Text(if (interactionState.favorited) "已收藏" else "收藏") },
        )
        AssistChip(onClick = onOpenComments, label = { Text("评论") })
        AssistChip(onClick = {}, label = { Text("分享") })
    }
}

@Composable
fun PlayerBottomInfo(
    title: String,
    description: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        if (tag.isNotBlank()) {
            AssistChip(onClick = {}, label = { Text(tag) })
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PlayerEpisodeDock(
    episodeCount: Int,
    currentEpisodeNumber: Int?,
    statusLabel: String,
    onOpenEpisodeSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenEpisodeSheet),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val current = currentEpisodeNumber?.let { "第 ${it} 集" } ?: "未选集"
            Text(text = "选集 · $statusLabel · 全 $episodeCount 集", style = MaterialTheme.typography.titleMedium)
            Text(text = current, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PlayerStatusContent(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SpeedPickerSheetContent(
    speeds: List<PlaybackSpeed>,
    currentSpeed: PlaybackSpeed,
    onSelectSpeed: (PlaybackSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "选择倍速", style = MaterialTheme.typography.titleLarge)
        speeds.forEach { speed ->
            FilterChip(
                selected = speed == currentSpeed,
                onClick = { onSelectSpeed(speed) },
                label = { Text(speed.label) },
            )
        }
    }
}

@Composable
fun EpisodePickerSheetContent(
    episodes: List<Episode>,
    currentEpisodeId: String?,
    onSelectEpisode: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "选集", style = MaterialTheme.typography.titleLarge)
        if (episodes.isEmpty()) {
            Text(text = "暂无剧集", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = episodes, key = { it.id }) { episode ->
                    val playable = episode.videoUrl.isNotBlank()
                    FilterChip(
                        selected = episode.id == currentEpisodeId,
                        onClick = { if (playable) onSelectEpisode(episode) },
                        enabled = playable,
                        label = {
                            Text(
                                text = buildString {
                                    append("第 ${episode.episodeNumber} 集")
                                    if (episode.title.isNotBlank()) {
                                        append(" · ${episode.title}")
                                    }
                                    if (!playable) {
                                        append(" · 暂无资源")
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
