package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.CheckInDayStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInDtosTest {

    @Test
    fun `T-01 sign in status dto maps snake case payload to domain`() {
        val dto = SignInStatusDto(
            serverDate = "2026-07-29",
            shouldShowPopup = true,
            todaySigned = false,
            currentStreak = 2,
            rewardCopy = "今日签到可领取第 3 天奖励",
            days = listOf(
                SignInDayDto(day = 1, title = "第 1 天", rewardLabel = "金币 x10", status = "signed"),
                SignInDayDto(day = 2, title = "第 2 天", rewardLabel = "金币 x20", status = "today"),
                SignInDayDto(day = 3, title = "第 3 天", rewardLabel = "金币 x30", status = "locked"),
            ),
        )

        val domain = dto.toDomain()

        assertEquals("2026-07-29", domain.serverDate)
        assertEquals(true, domain.shouldShowPopup)
        assertEquals(false, domain.todaySigned)
        assertEquals(2, domain.currentStreak)
        assertEquals("今日签到可领取第 3 天奖励", domain.rewardCopy)
        assertEquals(CheckInDayStatus.SIGNED, domain.days[0].status)
        assertEquals(CheckInDayStatus.TODAY, domain.days[1].status)
        assertEquals(CheckInDayStatus.LOCKED, domain.days[2].status)
    }
}
