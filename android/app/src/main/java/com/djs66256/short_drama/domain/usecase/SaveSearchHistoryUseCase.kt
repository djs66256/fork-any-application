package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.domain.repository.SearchRepository
import javax.inject.Inject

class SaveSearchHistoryUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(keyword: String) {
        searchRepository.saveSearchHistory(keyword)
    }
}
