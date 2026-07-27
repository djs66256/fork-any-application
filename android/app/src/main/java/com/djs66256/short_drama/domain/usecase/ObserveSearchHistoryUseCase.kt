package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.domain.repository.SearchRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSearchHistoryUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    operator fun invoke(): Flow<List<SearchHistoryItem>> = searchRepository.observeSearchHistory()
}
