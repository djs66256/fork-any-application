package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.CheckInLocalStore
import com.djs66256.short_drama.data.datasource.CheckInRemoteDataSource
import com.djs66256.short_drama.data.dto.SignInDayDto
import com.djs66256.short_drama.data.dto.SignInStatusDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInRepositoryImplTest {

    private val remoteDataSource = mockk<CheckInRemoteDataSource>()
    private val localStore = mockk<CheckInLocalStore>(relaxed = true)
    private val repository = CheckInRepositoryImpl(remoteDataSource, localStore)

    @Test
    fun `T-03 repository forwards installation id and maps status dto`() = runTest {
        coEvery { localStore.getOrCreateInstallationId() } returns "installation-123"
        coEvery { remoteDataSource.getCheckInStatus("installation-123") } returns ApiResult.Success(sampleStatusDto())

        val result = repository.getCheckInStatus()

        assertTrue(result is ApiResult.Success)
        val status = (result as ApiResult.Success).data
        assertEquals("2026-07-29", status.serverDate)
        assertEquals(3, status.days.size)
        coVerify { remoteDataSource.getCheckInStatus("installation-123") }
    }

    @Test
    fun `T-03 repository submitCheckIn maps dto and persists dismissed date`() = runTest {
        coEvery { localStore.getOrCreateInstallationId() } returns "installation-123"
        coEvery { remoteDataSource.submitCheckIn("installation-123") } returns ApiResult.Success(
            sampleStatusDto(todaySigned = true),
        )

        val result = repository.submitCheckIn()

        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data.todaySigned)
        coVerify { remoteDataSource.submitCheckIn("installation-123") }
    }

    @Test
    fun `T-03 repository keeps error semantics and exposes dismissed server date helpers`() = runTest {
        coEvery { localStore.getOrCreateInstallationId() } returns "installation-123"
        coEvery { remoteDataSource.getCheckInStatus("installation-123") } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "服务暂不可用",
        )
        coEvery { localStore.getDismissedServerDate() } returns "2026-07-29"

        val result = repository.getCheckInStatus()
        val dismissedDate = repository.getDismissedServerDate()
        repository.dismissForServerDate("2026-07-30")

        assertTrue(result is ApiResult.Error)
        assertEquals("2026-07-29", dismissedDate)
        coVerify { localStore.setDismissedServerDate("2026-07-30") }
    }

    private fun sampleStatusDto(todaySigned: Boolean = false): SignInStatusDto {
        return SignInStatusDto(
            serverDate = "2026-07-29",
            shouldShowPopup = !todaySigned,
            todaySigned = todaySigned,
            currentStreak = 2,
            rewardCopy = "今日签到可领取第 3 天奖励",
            days = listOf(
                SignInDayDto(day = 1, title = "第 1 天", rewardLabel = "金币 x10", status = "signed"),
                SignInDayDto(day = 2, title = "第 2 天", rewardLabel = "金币 x20", status = "signed"),
                SignInDayDto(
                    day = 3,
                    title = "第 3 天",
                    rewardLabel = "金币 x30",
                    status = if (todaySigned) "signed" else "today",
                ),
            ),
        )
    }
}
