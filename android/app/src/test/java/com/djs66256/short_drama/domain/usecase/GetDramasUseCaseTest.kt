package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.DramaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDramasUseCaseTest {

    private val dramaRepository: DramaRepository = mockk()
    private val useCase = GetDramasUseCase(dramaRepository)

    @Test
    fun `T-04 invoke delegates to repository and returns its result`() = runTest {
        val mockDramas = listOf(
            Drama(
                id = "1", title = "Test Drama", description = "Desc",
                coverUrl = "url", category = "action", episodeCount = 10,
                tags = listOf("tag1"), rating = 4.5, createdAt = "2024-01-01",
                updatedAt = "2024-01-02"
            )
        )
        val expectedResult = ApiResult.Success(mockDramas)

        coEvery { dramaRepository.getDramas(1, 20) } returns expectedResult

        val result = useCase(1, 20)

        coVerify(exactly = 1) { dramaRepository.getDramas(1, 20) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `T-04 invoke uses default parameters when none provided`() = runTest {
        coEvery { dramaRepository.getDramas(1, 20) } returns ApiResult.Success(emptyList())

        useCase()

        coVerify(exactly = 1) { dramaRepository.getDramas(1, 20) }
    }
}
