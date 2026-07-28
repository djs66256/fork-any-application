package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery
import com.djs66256.short_drama.domain.repository.DramaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTheaterFeedUseCaseTest {

    private val dramaRepository: DramaRepository = mockk()
    private val useCase = GetTheaterFeedUseCase(dramaRepository)

    @Test
    fun `T-02 invoke delegates theater query to repository`() = runTest {
        val query = TheaterQuery(channel = TheaterChannel.ALL, page = 1, pageSize = 20)
        val expected = ApiResult.Success(
            TheaterPage(
                channel = TheaterChannel.ALL,
                items = emptyList(),
                page = 1,
                pageSize = 20,
                total = 0,
                totalPages = 0,
            ),
        )
        coEvery { dramaRepository.getTheaterFeed(query) } returns expected

        val result = useCase(query)

        coVerify(exactly = 1) { dramaRepository.getTheaterFeed(query) }
        assertEquals(expected, result)
    }
}
