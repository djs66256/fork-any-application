package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.repository.MenuPanelRepository
import javax.inject.Inject

class GetRecentlyViewedUseCase @Inject constructor(
    private val menuPanelRepository: MenuPanelRepository,
) {
    suspend operator fun invoke(sessionId: String): ApiResult<List<RecentlyViewed>> {
        return menuPanelRepository.getRecentlyViewed(sessionId)
    }
}
