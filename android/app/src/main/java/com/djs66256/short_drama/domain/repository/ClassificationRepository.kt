package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.ClassificationTagsPayload

interface ClassificationRepository {
    suspend fun getClassificationTags(
        gender: ClassificationGender = ClassificationGender.ALL,
    ): ApiResult<ClassificationTagsPayload>
}
