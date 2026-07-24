package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Episode

/**
 * Repository interface for episode-related data operations.
 * Defined in the Domain layer, implemented in the Data layer.
 */
interface EpisodeRepository {
    suspend fun getEpisodeDetail(id: String): ApiResult<Episode>
}
