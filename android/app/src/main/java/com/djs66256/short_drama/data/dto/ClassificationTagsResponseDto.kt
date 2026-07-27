package com.djs66256.short_drama.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassificationTagsResponseDto(
    val data: ClassificationTagsPayloadDto,
)

@Serializable
data class ClassificationTagsPayloadDto(
    val gender: String,
    val dimensions: List<ClassificationDimensionDto>,
)

@Serializable
data class ClassificationDimensionDto(
    val key: String,
    val name: String,
    val tags: List<String> = emptyList(),
)
