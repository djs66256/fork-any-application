package com.djs66256.short_drama.feature.booking.model

import com.djs66256.short_drama.domain.model.BookingAsset
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.domain.model.BookingAssetSummary

data class BookingAssetItemUiModel(
    val dramaId: String,
    val title: String,
    val coverUrl: String,
    val episodeCountText: String,
    val bookedAtText: String,
    val statusLabel: String,
)

fun BookingAsset.toUiModel(): BookingAssetItemUiModel = BookingAssetItemUiModel(
    dramaId = dramaId,
    title = title,
    coverUrl = coverUrl,
    episodeCountText = if (episodeCount > 0) "共 ${episodeCount} 集" else "集数待更新",
    bookedAtText = formatBookedAt(bookedAt),
    statusLabel = availabilityStatus.label,
)

private fun formatBookedAt(rawValue: String): String {
    return rawValue.takeIf { it.isNotBlank() }?.let { "预约于 $it" } ?: "预约时间待更新"
}

sealed interface BookingAuthGate {
    data object Restoring : BookingAuthGate
    data object Anonymous : BookingAuthGate
    data object Authenticated : BookingAuthGate
    data object Expired : BookingAuthGate
}

data class BookingAssetsUiState(
    val selectedStatus: BookingAssetStatus = BookingAssetStatus.ONLINE,
    val summary: BookingAssetSummary = BookingAssetSummary(),
    val items: List<BookingAssetItemUiModel> = emptyList(),
    val authGate: BookingAuthGate = BookingAuthGate.Restoring,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val errorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
) {
    val showLoginGate: Boolean
        get() = authGate == BookingAuthGate.Anonymous || authGate == BookingAuthGate.Expired

    val showInitialLoading: Boolean
        get() = authGate == BookingAuthGate.Restoring || (isLoading && !hasLoadedOnce)

    val showContent: Boolean
        get() = items.isNotEmpty()

    val showEmpty: Boolean
        get() = hasLoadedOnce && items.isEmpty() && errorMessage == null && !showLoginGate

    val showError: Boolean
        get() = errorMessage != null && items.isEmpty() && !showLoginGate
}

sealed interface BookingAssetsEffect {
    data class RequireLogin(val returnRoute: String) : BookingAssetsEffect
    data class ShowMessage(val message: String) : BookingAssetsEffect
}
