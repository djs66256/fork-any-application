package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.ClassificationTagsPayload
import com.djs66256.short_drama.domain.repository.ClassificationRepository
import javax.inject.Inject

class GetClassificationTagsUseCase @Inject constructor(
    private val classificationRepository: ClassificationRepository,
) {
    suspend operator fun invoke(
        gender: ClassificationGender = ClassificationGender.ALL,
    ): ApiResult<ClassificationTagsPayload> = classificationRepository.getClassificationTags(gender)
}
