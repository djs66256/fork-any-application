package com.djs66256.short_drama.feature.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerInteractionState

private val PlayerSurfaceBlack = Color(0xFF000000)
private val PlayerSurfaceRaised = Color(0xFF161616)
private val PlayerSurfaceMuted = Color(0xFF252525)
private val PlayerTextPrimary = Color(0xFFF8F8F8)
private val PlayerTextSecondary = Color(0xFFCACACA)
private val PlayerTextMuted = Color(0xFF8A8A8A)
private val PlayerTrackMuted = Color(0xFF3C3C3C)

@Composable
fun PlayerTopBar(
    episodeLabel: String,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "返回",
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = episodeLabel,
                color = PlayerTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBarActionLabel(
                icon = Icons.Filled.Timer,
                label = "倍速",
                onClick = onToggleSpeedSheet,
            )
            IconButton(onClick = onToggleMore, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun TopBarActionLabel(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PlayerTextPrimary,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = label,
            color = PlayerTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PlayerCenterBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFF141414).copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Fullscreen,
            contentDescription = "全屏观看",
            tint = PlayerTextPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "全屏观看",
            color = PlayerTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PlayerRightActionBar(
    interactionState: PlayerInteractionState,
    favoriteCountLabel: String,
    commentCountLabel: String,
    likeCountLabel: String,
    shareCountLabel: String,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PlayerSideAction(
            icon = if (interactionState.favorited) Icons.Filled.Star else Icons.Filled.StarBorder,
            countLabel = favoriteCountLabel,
            onClick = onToggleFavorite,
        )
        PlayerSideAction(
            icon = Icons.Filled.ChatBubbleOutline,
            countLabel = commentCountLabel,
            onClick = onOpenComments,
        )
        PlayerSideAction(
            icon = if (interactionState.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            countLabel = likeCountLabel,
            onClick = onToggleLike,
        )
        PlayerSideAction(
            icon = Icons.Filled.Reply,
            countLabel = shareCountLabel,
            onClick = {},
        )
    }
}

@Composable
private fun PlayerSideAction(
    icon: ImageVector,
    countLabel: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = countLabel,
                tint = PlayerTextPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = countLabel,
            color = PlayerTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PlayerBottomInfo(
    title: String,
    hotComment: String,
    authorStatement: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            color = PlayerTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "热评",
                tint = PlayerTextPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = hotComment,
                color = PlayerTextSecondary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "作者声明",
                tint = PlayerTextMuted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = authorStatement,
                color = PlayerTextMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PlayerProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PlayerTrackMuted, CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(2.dp)
                .background(PlayerTextPrimary.copy(alpha = 0.96f), CircleShape),
        )
        Box(
            modifier = Modifier
                .padding(start = (280 * clampedProgress).dp)
                .size(7.dp)
                .background(PlayerTextPrimary, CircleShape),
        )
    }
}

@Composable
fun PlayerEpisodeDock(
    episodeLabel: String,
    onOpenEpisodeSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PlayerSurfaceRaised, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenEpisodeSheet)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = episodeLabel,
            color = PlayerTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "展开选集",
            tint = PlayerTextPrimary,
            modifier = Modifier.size(22.dp),
        )
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
            modifier = Modifier
                .background(Color(0xCC1A1A1A), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = PlayerTextPrimary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PlayerTextSecondary,
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    color = PlayerTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onAction),
                )
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
