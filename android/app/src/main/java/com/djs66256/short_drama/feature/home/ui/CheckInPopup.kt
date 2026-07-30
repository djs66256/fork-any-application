package com.djs66256.short_drama.feature.home.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.core.theme.CheckInPopupCream
import com.djs66256.short_drama.core.theme.CheckInPopupDayLocked
import com.djs66256.short_drama.core.theme.CheckInPopupDaySigned
import com.djs66256.short_drama.core.theme.CheckInPopupDayToday
import com.djs66256.short_drama.core.theme.CheckInPopupOrangeDeep
import com.djs66256.short_drama.core.theme.CheckInPopupOrangeEnd
import com.djs66256.short_drama.core.theme.CheckInPopupOrangeStart
import com.djs66256.short_drama.core.theme.CheckInPopupOutline
import com.djs66256.short_drama.core.theme.CheckInPopupPanel
import com.djs66256.short_drama.core.theme.CheckInPopupTextDark
import com.djs66256.short_drama.core.theme.HomeFeedScrim
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
            .background(HomeFeedScrim.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CheckInPopupOrangeStart,
                                    CheckInPopupOrangeEnd,
                                ),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 22.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "关闭签到浮层",
                                    tint = Color.White,
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = buildCheckInHeadline(state.currentStreak),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = buildCheckInSubline(state.serverDate),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.88f),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = CheckInPopupPanel,
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                userScrollEnabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                            ) {
                                items(state.days, key = { it.day }) { day ->
                                    CheckInDayCell(day = day)
                                }
                            }
                        }

                        if (state.submitErrorMessage != null) {
                            Text(
                                text = state.submitErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CheckInPopupTextDark,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Button(
                            onClick = onSubmit,
                            enabled = !state.isSubmitting && !state.todaySigned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CheckInPopupCream,
                                contentColor = CheckInPopupTextDark,
                                disabledContainerColor = CheckInPopupCream.copy(alpha = 0.75f),
                                disabledContentColor = CheckInPopupTextDark.copy(alpha = 0.7f),
                            ),
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CheckInPopupTextDark,
                                )
                            } else {
                                Text(
                                    text = if (state.todaySigned) "今日已签到" else "立即领取",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Text(
                            text = state.rewardCopy.ifBlank { "金币奖励可在「福利」查看" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInDayCell(day: CheckInDay) {
    val containerColor = when (day.status) {
        CheckInDayStatus.SIGNED -> CheckInPopupDaySigned
        CheckInDayStatus.TODAY -> CheckInPopupDayToday
        CheckInDayStatus.LOCKED -> CheckInPopupDayLocked
    }
    val contentColor = when (day.status) {
        CheckInDayStatus.TODAY -> Color.White
        else -> CheckInPopupTextDark
    }
    val iconTint = if (day.status == CheckInDayStatus.TODAY) {
        Color(0xFFFFF0D7)
    } else {
        CheckInPopupOrangeDeep
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, CheckInPopupOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = extractRewardNumber(day.rewardLabel),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = if (day.status == CheckInDayStatus.TODAY) 0.18f else 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (day.day % 7 == 0) Icons.Filled.EmojiEvents else Icons.Filled.Star,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = normalizeDayTitle(day.title, day.day),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when (day.status) {
                    CheckInDayStatus.SIGNED -> "已领取"
                    CheckInDayStatus.TODAY -> "今日可领"
                    CheckInDayStatus.LOCKED -> "待开启"
                },
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun buildCheckInHeadline(currentStreak: Int): String {
    val streakDays = currentStreak.coerceAtLeast(6) + 1
    return "${streakDays}天签到必得6万金币"
}

private fun buildCheckInSubline(serverDate: String?): String {
    val serverMonthDay = serverDate
        ?.split("-")
        ?.takeLast(2)
        ?.joinToString("/")
        ?: "07/22"
    return "$serverMonthDay 起连续登录，轻松入账"
}

private fun extractRewardNumber(rewardLabel: String): String {
    val digits = rewardLabel.filter(Char::isDigit)
    return if (digits.isNotEmpty()) digits else rewardLabel.ifBlank { "5888" }
}

private fun normalizeDayTitle(title: String, day: Int): String {
    return title
        .replace(" ", "")
        .ifBlank { "第${day}天" }
}
