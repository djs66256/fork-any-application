package com.djs66256.short_drama.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInDayStatus
import com.djs66256.short_drama.feature.home.viewmodel.CheckInPopupUiState

@Composable
fun CheckInPopup(
    state: CheckInPopupUiState,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "连续签到 ${state.currentStreak} 天",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                            if (state.serverDate != null) {
                                Text(
                                    text = "服务端业务日 ${state.serverDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭签到浮层",
                            )
                        }
                    }

                    Text(
                        text = state.rewardCopy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(state.days, key = { it.day }) { day ->
                            CheckInDayCell(day = day)
                        }
                    }

                    state.submitErrorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (state.todaySigned) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("今日已签到")
                        }
                    } else {
                        Button(
                            onClick = onSubmit,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("立即签到")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInDayCell(day: CheckInDay) {
    val containerColor = when (day.status) {
        CheckInDayStatus.SIGNED -> MaterialTheme.colorScheme.primaryContainer
        CheckInDayStatus.TODAY -> MaterialTheme.colorScheme.tertiaryContainer
        CheckInDayStatus.LOCKED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when (day.status) {
        CheckInDayStatus.SIGNED -> MaterialTheme.colorScheme.onPrimaryContainer
        CheckInDayStatus.TODAY -> MaterialTheme.colorScheme.onTertiaryContainer
        CheckInDayStatus.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = day.title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = day.rewardLabel,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when (day.status) {
                    CheckInDayStatus.SIGNED -> "已签到"
                    CheckInDayStatus.TODAY -> "今日可签"
                    CheckInDayStatus.LOCKED -> "待解锁"
                },
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
