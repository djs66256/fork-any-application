package com.djs66256.short_drama.feature.booking.ui.components

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.domain.model.BookingAssetSummary

@Composable
fun BookingStatusTabs(
    selectedStatus: BookingAssetStatus,
    summary: BookingAssetSummary,
    onStatusSelected: (BookingAssetStatus) -> Unit,
) {
    val statuses = BookingAssetStatus.entries
    TabRow(selectedTabIndex = statuses.indexOf(selectedStatus)) {
        statuses.forEach { status ->
            Tab(
                selected = status == selectedStatus,
                onClick = { onStatusSelected(status) },
                text = { Text("${status.label}(${summary.countFor(status)})") },
            )
        }
    }
}
