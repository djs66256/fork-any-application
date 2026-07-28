package com.djs66256.short_drama.feature.classification.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.ClassificationDimension
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.ClassificationTagsPayload
import com.djs66256.short_drama.domain.usecase.GetClassificationTagsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassificationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getClassificationTagsUseCase = mockk<GetClassificationTagsUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-01 default load keeps fixed three dimensions and first dimension selected`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returns ApiResult.Success(
            payload(
                gender = ClassificationGender.ALL,
                dimensions = listOf(
                    dimension(ClassificationDimensionKey.ERA_BACKGROUND, tags = listOf("都市", "校园")),
                    dimension(ClassificationDimensionKey.THEME_PLOT, tags = emptyList()),
                    dimension(ClassificationDimensionKey.CHARACTER_SETTING, tags = listOf("萌宝")),
                ),
            ),
        )

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ClassificationGender.ALL, state.selectedGender)
        assertEquals(ClassificationDimensionKey.ERA_BACKGROUND, state.selectedDimensionKey)
        assertEquals(3, state.dimensions.size)
        assertEquals(
            listOf(
                ClassificationDimensionKey.ERA_BACKGROUND,
                ClassificationDimensionKey.THEME_PLOT,
                ClassificationDimensionKey.CHARACTER_SETTING,
            ),
            state.dimensions.map { it.key },
        )
        assertTrue(state.dimensions[1].tags.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.hasLoadedOnce)
    }

    @Test
    fun `T-02 first load failure and retry recovers successfully`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returnsMany listOf(
            ApiResult.Error(code = "INTERNAL_ERROR", message = "首次失败"),
            ApiResult.Success(payload(gender = ClassificationGender.ALL)),
        )

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        assertEquals("首次失败", viewModel.uiState.value.errorMessage)
        assertEquals(3, viewModel.uiState.value.dimensions.size)
        assertTrue(viewModel.uiState.value.dimensions.all { it.tags.isEmpty() })

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(3, state.dimensions.size)
        assertFalse(state.isLoading)
        assertTrue(state.hasLoadedOnce)
        coVerify(exactly = 2) { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) }
    }

    @Test
    fun `T-03 switching gender resets dimension and emits scroll to first dimension`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returns ApiResult.Success(
            payload(gender = ClassificationGender.ALL),
        )
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.MALE) } returns ApiResult.Success(
            payload(
                gender = ClassificationGender.MALE,
                dimensions = listOf(
                    dimension(ClassificationDimensionKey.ERA_BACKGROUND, tags = listOf("玄幻")),
                    dimension(ClassificationDimensionKey.THEME_PLOT, tags = listOf("逆袭")),
                    dimension(ClassificationDimensionKey.CHARACTER_SETTING, tags = listOf("龙王")),
                ),
            ),
        )

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()
        viewModel.onDimensionSelected(ClassificationDimensionKey.CHARACTER_SETTING)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onGenderSelected(ClassificationGender.MALE)
            advanceUntilIdle()

            assertEquals(
                ClassificationEffect.ScrollToDimension(ClassificationDimensionKey.ERA_BACKGROUND),
                awaitItem(),
            )
        }

        val state = viewModel.uiState.value
        assertEquals(ClassificationGender.MALE, state.selectedGender)
        assertEquals(ClassificationDimensionKey.ERA_BACKGROUND, state.selectedDimensionKey)
        assertEquals(listOf("玄幻"), state.dimensions[0].tags)
    }

    @Test
    fun `T-04 quick gender switch only applies latest response`() = runTest {
        val maleGate = CompletableDeferred<Unit>()
        val femaleGate = CompletableDeferred<Unit>()
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returns ApiResult.Success(
            payload(gender = ClassificationGender.ALL),
        )
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.MALE) } coAnswers {
            maleGate.await()
            ApiResult.Success(
                payload(
                    gender = ClassificationGender.MALE,
                    dimensions = listOf(
                        dimension(ClassificationDimensionKey.ERA_BACKGROUND, tags = listOf("玄幻")),
                        dimension(ClassificationDimensionKey.THEME_PLOT, tags = listOf("逆袭")),
                        dimension(ClassificationDimensionKey.CHARACTER_SETTING, tags = listOf("龙王")),
                    ),
                ),
            )
        }
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.FEMALE) } coAnswers {
            femaleGate.await()
            ApiResult.Success(
                payload(
                    gender = ClassificationGender.FEMALE,
                    dimensions = listOf(
                        dimension(ClassificationDimensionKey.ERA_BACKGROUND, tags = listOf("校园")),
                        dimension(ClassificationDimensionKey.THEME_PLOT, tags = listOf("甜宠")),
                        dimension(ClassificationDimensionKey.CHARACTER_SETTING, tags = listOf("萌宝")),
                    ),
                ),
            )
        }

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        val maleJob = async { viewModel.onGenderSelected(ClassificationGender.MALE) }
        runCurrent()
        val femaleJob = async { viewModel.onGenderSelected(ClassificationGender.FEMALE) }
        runCurrent()

        femaleGate.complete(Unit)
        advanceUntilIdle()
        maleGate.complete(Unit)
        maleJob.await()
        femaleJob.await()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ClassificationGender.FEMALE, state.selectedGender)
        assertEquals(listOf("校园"), state.dimensions[0].tags)
        assertEquals(listOf("甜宠"), state.dimensions[1].tags)
        assertEquals(listOf("萌宝"), state.dimensions[2].tags)
    }

    @Test
    fun `T-05 dimension click and visible section update keep anchor synchronized`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returns ApiResult.Success(
            payload(gender = ClassificationGender.ALL),
        )

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onDimensionSelected(ClassificationDimensionKey.THEME_PLOT)
            advanceUntilIdle()

            assertEquals(
                ClassificationEffect.ScrollToDimension(ClassificationDimensionKey.THEME_PLOT),
                awaitItem(),
            )
        }
        assertEquals(ClassificationDimensionKey.THEME_PLOT, viewModel.uiState.value.selectedDimensionKey)

        viewModel.onVisibleDimensionChanged(ClassificationDimensionKey.CHARACTER_SETTING)
        assertEquals(ClassificationDimensionKey.CHARACTER_SETTING, viewModel.uiState.value.selectedDimensionKey)
    }

    @Test
    fun `T-06 buildSearchRoute reuses normalizeSearchQueryOrNull and search result route`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(any()) } returns ApiResult.Success(payload())

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        assertEquals(
            "search/result?query=%E8%90%8C%E5%AE%9D",
            viewModel.buildSearchRoute(" 萌宝 "),
        )
        if (viewModel.buildSearchRoute("   ") != null) {
            fail("空白标签不应生成 route")
        }
    }

    @Test
    fun `T-06 refresh failure after content keeps previous dimensions and shows error`() = runTest {
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.ALL) } returns ApiResult.Success(payload())
        coEvery { getClassificationTagsUseCase.invoke(ClassificationGender.MALE) } returns ApiResult.Error(
            code = "INTERNAL_ERROR",
            message = "切换失败",
        )

        val viewModel = ClassificationViewModel(
            SavedStateHandle(),
            getClassificationTagsUseCase,
        )
        advanceUntilIdle()

        viewModel.onGenderSelected(ClassificationGender.MALE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ClassificationGender.MALE, state.selectedGender)
        assertEquals(3, state.dimensions.size)
        assertEquals("切换失败", state.errorMessage)
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoading)
    }

    private fun payload(
        gender: ClassificationGender = ClassificationGender.ALL,
        dimensions: List<ClassificationDimension> = listOf(
            dimension(ClassificationDimensionKey.ERA_BACKGROUND, tags = listOf("都市", "校园")),
            dimension(ClassificationDimensionKey.THEME_PLOT, tags = listOf("逆袭", "甜宠")),
            dimension(ClassificationDimensionKey.CHARACTER_SETTING, tags = listOf("大女主", "萌宝")),
        ),
    ): ClassificationTagsPayload = ClassificationTagsPayload(
        gender = gender,
        dimensions = dimensions,
    )

    private fun dimension(
        key: ClassificationDimensionKey,
        tags: List<String>,
    ): ClassificationDimension = ClassificationDimension(
        key = key,
        name = key.label,
        tags = tags,
    )
}
