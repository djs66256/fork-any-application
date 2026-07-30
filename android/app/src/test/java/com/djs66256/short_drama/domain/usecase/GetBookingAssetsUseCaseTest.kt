package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.model.BookingAssetSummary
import com.djs66256.short_drama.domain.repository.DramaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBookingAssetsUseCaseTest {

    private val dramaRepository: DramaRepository = mockk()
    private val useCase = GetBookingAssetsUseCase(dramaRepository)

    @Test
    fun `T-11 invoke delegates booking query to repository`() = runTest {
        val query = BookingAssetsQuery()
        val expected = ApiResult.Success(
            BookingAssetsPage(
                items = emptyList(),
                page = 1,
                pageSize = 20,
                total = 0,
                totalPages = 0,
                summary = BookingAssetSummary(onlineCount = 2, upcomingCount = 3),
            ),
        )
        coEvery { dramaRepository.getBookingAssets(query) } returns expected

        val result = useCase(query)

        coVerify(exactly = 1) { dramaRepository.getBookingAssets(query) }
        assertEquals(expected, result)
    }
}
