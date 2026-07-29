package com.djs66256.short_drama.feature.comments.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.model.CommentPage
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.domain.model.CommentUser
import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.domain.usecase.CreateCommentUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaCommentsUseCase
import com.djs66256.short_drama.domain.usecase.ToggleCommentLikeUseCase
import com.djs66256.short_drama.feature.comments.model.CommentPendingActionType
import com.djs66256.short_drama.feature.comments.model.CommentSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentSheetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getDramaCommentsUseCase = mockk<GetDramaCommentsUseCase>()
    private val createCommentUseCase = mockk<CreateCommentUseCase>()
    private val toggleCommentLikeUseCase = mockk<ToggleCommentLikeUseCase>()
    private val authSessionProvider = mockk<AuthSessionProvider>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-03 open loads first page into content state`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(dramaId = "drama-1", page = 1, pageSize = 20, sort = CommentSort.LATEST),
            )
        } returns ApiResult.Success(commentPage(items = listOf(comment("comment-1")), total = 36, totalPages = 2))

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CommentListState.Content, state.listState)
        assertEquals(1, state.comments.size)
        assertEquals("comment-1", state.comments.single().id)
        assertEquals(36, state.totalCount)
        assertTrue(state.hasNextPage)
        assertEquals(CommentSource.PLAYER, state.source)
    }

    @Test
    fun `T-03 open empty result enters empty state`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(items = emptyList(), total = 0, totalPages = 0),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CommentListState.Empty, state.listState)
        assertTrue(state.comments.isEmpty())
        assertEquals(0, state.totalCount)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun `T-03 open failure enters error state with retry`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery { getDramaCommentsUseCase(any()) } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败"),
            ApiResult.Success(commentPage(items = listOf(comment("comment-2")), total = 1, totalPages = 1)),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()
        assertEquals(CommentListState.Error, viewModel.uiState.value.listState)
        assertEquals("首次失败", viewModel.uiState.value.errorMessage)

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CommentListState.Content, state.listState)
        assertEquals(listOf("comment-2"), state.comments.map { it.id })
    }

    @Test
    fun `T-04 selectSort resets list and requests hot first page`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(items = listOf(comment("latest-1")), total = 3, totalPages = 2),
        )
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.HOT,
                ),
            )
        } returns ApiResult.Success(
            commentPage(items = listOf(comment("hot-1")), total = 1, totalPages = 1),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        viewModel.onInputChanged("待清空的输入")
        viewModel.selectSort(CommentSort.HOT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CommentSort.HOT, state.selectedSort)
        assertEquals(listOf("hot-1"), state.comments.map { it.id })
        assertEquals("", state.inputText)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun `T-04 loadNextPage appends items and keeps content state`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(items = listOf(comment("comment-1")), total = 3, totalPages = 2),
        )
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 2,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(
                items = listOf(comment("comment-2"), comment("comment-3")),
                page = 2,
                total = 3,
                totalPages = 2,
            ),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CommentListState.Content, state.listState)
        assertEquals(listOf("comment-1", "comment-2", "comment-3"), state.comments.map { it.id })
        assertEquals(2, state.page)
        assertFalse(state.hasNextPage)
        assertNull(state.appendErrorMessage)
    }

    @Test
    fun `T-04 loadNextPage failure keeps old items and exposes footer error`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(items = listOf(comment("comment-1")), total = 2, totalPages = 2),
        )
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 2,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Error(code = "SERVICE_UNAVAILABLE", message = "分页失败")

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("comment-1"), state.comments.map { it.id })
        assertEquals("分页失败", state.appendErrorMessage)
        assertFalse(state.isAppending)
        assertEquals(CommentListState.Content, state.listState)
    }

    @Test
    fun `T-05 logged in submit inserts comment at top and clears composer`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns true
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(items = listOf(comment("comment-1")), total = 1, totalPages = 1),
        )
        coEvery { createCommentUseCase("drama-1", "新的评论") } returns ApiResult.Success(
            comment(id = "comment-new", content = "新的评论"),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()
        viewModel.onInputChanged("  新的评论  ")

        viewModel.submitComment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("comment-new", "comment-1"), state.comments.map { it.id })
        assertEquals("", state.inputText)
        assertEquals(2, state.totalCount)
        assertFalse(state.isSubmitting)
        coVerify(exactly = 1) { createCommentUseCase("drama-1", "新的评论") }
    }

    @Test
    fun `T-05 blank submit is blocked locally without request`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns true
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(items = emptyList(), total = 0, totalPages = 0),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()
        viewModel.onInputChanged("   ")

        viewModel.submitComment()
        advanceUntilIdle()

        assertEquals("评论内容不能为空", viewModel.uiState.value.composerErrorMessage)
        coVerify(exactly = 0) { createCommentUseCase(any(), any()) }
    }

    @Test
    fun `T-06 anonymous submit emits require login context without replay`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(items = emptyList(), total = 0, totalPages = 0),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()
        viewModel.onInputChanged("评论")

        viewModel.effects.test {
            viewModel.submitComment()
            val effect = awaitItem() as CommentEffect.RequireLogin
            assertEquals(CommentSource.HOME, effect.context.source)
            assertEquals("drama-1", effect.context.dramaId)
            assertEquals("home", effect.context.returnRoute)
            assertEquals(CommentPendingActionType.CREATE_COMMENT, effect.context.action.type)
            assertNull(effect.context.action.commentId)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("评论", viewModel.uiState.value.inputText)
        coVerify(exactly = 0) { createCommentUseCase(any(), any()) }
    }

    @Test
    fun `T-06 anonymous toggle like emits require login with comment id`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns false
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(items = listOf(comment("comment-1")), total = 1, totalPages = 1),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.toggleLike("comment-1")
            val effect = awaitItem() as CommentEffect.RequireLogin
            assertEquals(CommentSource.PLAYER, effect.context.source)
            assertEquals("play/drama-1", effect.context.returnRoute)
            assertEquals(CommentPendingActionType.TOGGLE_LIKE, effect.context.action.type)
            assertEquals("comment-1", effect.context.action.commentId)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { toggleCommentLikeUseCase(any(), any()) }
    }

    @Test
    fun `T-08 toggleLike updates only target item and single-flight lock blocks duplicates`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns true
        val gate = CompletableDeferred<Unit>()
        coEvery { getDramaCommentsUseCase(any()) } returns ApiResult.Success(
            commentPage(
                items = listOf(comment("comment-1", liked = false, likeCount = 1)),
                total = 1,
                totalPages = 1,
            ),
        )
        coEvery { toggleCommentLikeUseCase("drama-1", "comment-1") } coAnswers {
            gate.await()
            ApiResult.Success(ToggleCommentLikeResult(commentId = "comment-1", liked = true, likeCount = 2))
        }

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.PLAYER)
        advanceUntilIdle()

        val first = async { viewModel.toggleLike("comment-1") }
        runCurrent()
        val duringRequest = viewModel.uiState.value
        assertTrue("comment-1" in duringRequest.likingCommentIds)

        val second = async { viewModel.toggleLike("comment-1") }
        runCurrent()
        gate.complete(Unit)
        first.await()
        second.await()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.comments.single().liked)
        assertEquals(2, state.comments.single().likeCount)
        assertTrue(state.likingCommentIds.isEmpty())
        coVerify(exactly = 1) { toggleCommentLikeUseCase("drama-1", "comment-1") }
    }

    @Test
    fun `T-08 opening another drama resets comments and input context`() = runTest {
        every { authSessionProvider.isLoggedIn() } returns true
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-1",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(items = listOf(comment("comment-1")), total = 1, totalPages = 1),
        )
        coEvery {
            getDramaCommentsUseCase(
                CommentQuery(
                    dramaId = "drama-2",
                    page = 1,
                    pageSize = 20,
                    sort = CommentSort.LATEST,
                ),
            )
        } returns ApiResult.Success(
            commentPage(
                items = listOf(comment("comment-2")),
                total = 1,
                totalPages = 1,
                dramaId = "drama-2",
            ),
        )

        val viewModel = createViewModel()
        viewModel.open(dramaId = "drama-1", source = CommentSource.HOME)
        advanceUntilIdle()
        viewModel.onInputChanged("上一部剧的输入")

        viewModel.open(dramaId = "drama-2", source = CommentSource.PLAYER)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("drama-2", state.dramaId)
        assertEquals(CommentSource.PLAYER, state.source)
        assertEquals(listOf("comment-2"), state.comments.map { it.id })
        assertEquals("", state.inputText)
    }

    private fun createViewModel(): CommentSheetViewModel {
        return CommentSheetViewModel(
            getDramaCommentsUseCase = getDramaCommentsUseCase,
            createCommentUseCase = createCommentUseCase,
            toggleCommentLikeUseCase = toggleCommentLikeUseCase,
            authSessionProvider = authSessionProvider,
        )
    }

    private fun commentPage(
        items: List<Comment>,
        page: Int = 1,
        total: Int,
        totalPages: Int,
        dramaId: String = "drama-1",
    ): CommentPage {
        return CommentPage(
            dramaId = dramaId,
            items = items,
            page = page,
            pageSize = 20,
            total = total,
            totalPages = totalPages,
        )
    }

    private fun comment(
        id: String,
        content: String = "评论正文",
        liked: Boolean = false,
        likeCount: Int = 1,
    ): Comment {
        return Comment(
            id = id,
            dramaId = "drama-1",
            content = content,
            likeCount = likeCount,
            liked = liked,
            createdAt = "2026-07-29T09:30:00.000Z",
            updatedAt = "2026-07-29T09:30:00.000Z",
            user = CommentUser(
                id = "user-1",
                displayName = "用户昵称",
                avatarUrl = null,
            ),
        )
    }
}
