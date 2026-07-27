package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.ClassificationRemoteDataSource
import com.djs66256.short_drama.data.dto.ClassificationDimensionDto
import com.djs66256.short_drama.data.dto.ClassificationTagsPayloadDto
import com.djs66256.short_drama.data.dto.ClassificationTagsResponseDto
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificationRepositoryImplTest {
    private val remoteDataSource = mockk<ClassificationRemoteDataSource>()
    private val repository = ClassificationRepositoryImpl(remoteDataSource)

    @Test
    fun `T-07 getClassificationTags maps fixed dimensions and keeps empty dimension`() = runTest {
        coEvery { remoteDataSource.getClassificationTags("female") } returns ApiResult.Success(
            ClassificationTagsResponseDto(
                data = ClassificationTagsPayloadDto(
                    gender = "female",
                    dimensions = listOf(
                        ClassificationDimensionDto(
                            key = "era_background",
                            name = "时代背景",
                            tags = listOf("都市"),
                        ),
                        ClassificationDimensionDto(
                            key = "theme_plot",
                            name = "主题情节",
                            tags = emptyList(),
                        ),
                        ClassificationDimensionDto(
                            key = "character_setting",
                            name = "角色设定",
                            tags = listOf("萌宝"),
                        ),
                    ),
                ),
            ),
        )

        val result = repository.getClassificationTags(ClassificationGender.FEMALE)

        assertTrue(result is ApiResult.Success)
        val payload = (result as ApiResult.Success).data
        assertEquals(ClassificationGender.FEMALE, payload.gender)
        assertEquals(
            listOf(
                ClassificationDimensionKey.ERA_BACKGROUND,
                ClassificationDimensionKey.THEME_PLOT,
                ClassificationDimensionKey.CHARACTER_SETTING,
            ),
            payload.dimensions.map { it.key },
        )
        assertTrue(payload.dimensions[1].tags.isEmpty())
    }

    @Test
    fun `T-07 repository fills missing dimension with empty tags and default title`() = runTest {
        coEvery { remoteDataSource.getClassificationTags("male") } returns ApiResult.Success(
            ClassificationTagsResponseDto(
                data = ClassificationTagsPayloadDto(
                    gender = "male",
                    dimensions = listOf(
                        ClassificationDimensionDto(
                            key = "theme_plot",
                            name = "主题情节",
                            tags = listOf("逆袭"),
                        ),
                    ),
                ),
            ),
        )

        val result = repository.getClassificationTags(ClassificationGender.MALE)

        assertTrue(result is ApiResult.Success)
        val payload = (result as ApiResult.Success).data
        assertEquals("时代背景", payload.dimensions[0].name)
        assertTrue(payload.dimensions[0].tags.isEmpty())
        assertEquals("主题情节", payload.dimensions[1].name)
        assertEquals(listOf("逆袭"), payload.dimensions[1].tags)
        assertEquals("角色设定", payload.dimensions[2].name)
        assertTrue(payload.dimensions[2].tags.isEmpty())
    }

    @Test
    fun `T-07 repository forwards gender api value to remote data source`() = runTest {
        coEvery { remoteDataSource.getClassificationTags("all") } returns ApiResult.Success(
            ClassificationTagsResponseDto(
                data = ClassificationTagsPayloadDto(
                    gender = "all",
                    dimensions = emptyList(),
                ),
            ),
        )

        repository.getClassificationTags(ClassificationGender.ALL)

        io.mockk.coVerify(exactly = 1) { remoteDataSource.getClassificationTags("all") }
    }
}
