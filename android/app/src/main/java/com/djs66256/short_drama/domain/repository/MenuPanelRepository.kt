package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.RecentlyViewed

interface MenuPanelRepository {
    suspend fun getRecentlyViewed(sessionId: String): ApiResult<List<RecentlyViewed>>
}
