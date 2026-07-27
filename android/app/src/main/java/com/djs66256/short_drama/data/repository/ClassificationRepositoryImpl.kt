package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.ClassificationRemoteDataSource
import com.djs66256.short_drama.data.dto.ClassificationDimensionDto
import com.djs66256.short_drama.data.dto.ClassificationTagsResponseDto
import com.djs66256.short_drama.domain.model.ClassificationDimension
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.ClassificationTagsPayload
import com.djs66256.short_drama.domain.repository.ClassificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassificationRepositoryImpl @Inject constructor(
    private val remoteDataSource: ClassificationRemoteDataSource,
) : ClassificationRepository {
    override suspend fun getClassificationTags(
        gender: ClassificationGender,
    ): ApiResult<ClassificationTagsPayload> {
        return when (val result = remoteDataSource.getClassificationTags(gender.apiValue)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}

private fun ClassificationTagsResponseDto.toDomain(): ClassificationTagsPayload {
    val dimensionMap = data.dimensions.associateBy { it.key }
    val dimensions = ClassificationDimensionKey.entries.map { key ->
        dimensionMap[key.apiValue].toDomain(key)
    }
    return ClassificationTagsPayload(
        gender = ClassificationGender.fromApiValue(data.gender),
        dimensions = dimensions,
    )
}

private fun ClassificationDimensionDto?.toDomain(key: ClassificationDimensionKey): ClassificationDimension {
    return ClassificationDimension(
        key = key,
        name = this?.name?.trim().orEmpty().ifBlank { key.label },
        tags = this?.tags ?: emptyList(),
    )
}
