package com.djs66256.short_drama.feature.messages.viewmodel

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.InteractionMessageType
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.model.SystemMessage
import com.djs66256.short_drama.domain.usecase.GetInteractionMessagesUseCase
import com.djs66256.short_drama.domain.usecase.GetSystemMessagesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageCenterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authStateFlow = MutableStateFlow<AuthStatus>(AuthStatus.Anonymous)
    private val authStateHolder = mockk<AuthStateHolder>()
    private val getSystemMessagesUseCase = mockk<GetSystemMessagesUseCase>()
    private val getInteractionMessagesUseCase = mockk<GetInteractionMessagesUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authStateHolder.authStatus } returns authStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-06 anonymous state loads system messages and shows login gate`() = runTest {
        coEvery { getSystemMessagesUseCase(page = 1, pageSize = 20) } returns ApiResult.Success(
            MessagePage(
                items = listOf(sampleSystemMessage()),
                page = 1,
                pageSize = 20,
                total = 1,
                totalPages = 1,
            ),
        )

        val viewModel = MessageCenterViewModel(
            authStateHolder = authStateHolder,
            getSystemMessagesUseCase = getSystemMessagesUseCase,
            getInteractionMessagesUseCase = getInteractionMessagesUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.systemMessages.size)
        assertTrue(state.showInteractionLoginGate)
        assertFalse(state.isInteractionLoading)
        coVerify(exactly = 0) { getInteractionMessagesUseCase(any(), any()) }
    }

    @Test
    fun `T-06 authenticated state loads both sections`() = runTest {
        authStateFlow.value = AuthStatus.Authenticated(sampleSession())
        coEvery { getSystemMessagesUseCase(page = 1, pageSize = 20) } returns ApiResult.Success(
            MessagePage(
                items = listOf(sampleSystemMessage()),
                page = 1,
                pageSize = 20,
                total = 1,
                totalPages = 1,
            ),
        )
        coEvery { getInteractionMessagesUseCase(page = 1, pageSize = 20) } returns ApiResult.Success(
            MessagePage(
                items = listOf(sampleInteractionMessage()),
                page = 1,
                pageSize = 20,
                total = 1,
                totalPages = 1,
            ),
        )

        val viewModel = MessageCenterViewModel(
            authStateHolder = authStateHolder,
            getSystemMessagesUseCase = getSystemMessagesUseCase,
            getInteractionMessagesUseCase = getInteractionMessagesUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showInteractionLoginGate)
        assertEquals(1, state.systemMessages.size)
        assertEquals(1, state.interactionMessages.size)
    }

    @Test
    fun `T-06 interaction error remains local to interaction section`() = runTest {
        authStateFlow.value = AuthStatus.Authenticated(sampleSession())
        coEvery { getSystemMessagesUseCase(page = 1, pageSize = 20) } returns ApiResult.Success(
            MessagePage(
                items = listOf(sampleSystemMessage()),
                page = 1,
                pageSize = 20,
                total = 1,
                totalPages = 1,
            ),
        )
        coEvery { getInteractionMessagesUseCase(page = 1, pageSize = 20) } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "互动消息加载失败",
        )

        val viewModel = MessageCenterViewModel(
            authStateHolder = authStateHolder,
            getSystemMessagesUseCase = getSystemMessagesUseCase,
            getInteractionMessagesUseCase = getInteractionMessagesUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.systemMessages.size)
        assertEquals("互动消息加载失败", state.interactionErrorMessage)
        assertTrue(state.interactionMessages.isEmpty())
        assertFalse(state.showInteractionLoginGate)
    }

    private fun sampleSession(): AuthSession {
        return AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtIso = "2026-07-30T00:00:00Z",
            user = AuthUser(
                id = "user-1",
                phone = "13800138000",
                displayName = "Daniel",
                avatarUrl = null,
                role = AuthRole.VIEWER,
                isNewUser = false,
            ),
        )
    }

    private fun sampleSystemMessage(): SystemMessage {
        return SystemMessage(
            id = "system-1",
            title = "系统通知",
            summary = "你关注的剧集已更新第 12 集。",
            sentAt = "2026-07-29T08:00:00.000Z",
        )
    }

    private fun sampleInteractionMessage(): InteractionMessage {
        return InteractionMessage(
            id = "interaction-1",
            type = InteractionMessageType.COMMENT_REPLY,
            title = "有人回复了你的评论",
            summary = "收到一条新回复。",
            sentAt = "2026-07-29T09:00:00.000Z",
        )
    }
}
