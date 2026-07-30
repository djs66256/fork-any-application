package com.djs66256.short_drama.feature.booking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.BookingAssetStatus

@Composable
fun BookingAssetsEmptyState(
    status: BookingAssetStatus,
    modifier: Modifier = Modifier,
) {
    val title = when (status) {
        BookingAssetStatus.ONLINE -> "暂无已上线预约"
        BookingAssetStatus.UPCOMING -> "暂无待上线预约"
    }
    val description = when (status) {
        BookingAssetStatus.ONLINE -> "你已预约的内容上线后会显示在这里。"
        BookingAssetStatus.UPCOMING -> "还没有待上线的预约内容，去排行页看看新剧吧。"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
