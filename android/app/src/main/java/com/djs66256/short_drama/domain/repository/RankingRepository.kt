package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookDramaResult
import com.djs66256.short_drama.domain.model.RankingPage
import com.djs66256.short_drama.domain.model.RankingQuery

interface RankingRepository {
    suspend fun getDramaRankings(query: RankingQuery): ApiResult<RankingPage>

    suspend fun bookDrama(dramaId: String): ApiResult<BookDramaResult>
}
