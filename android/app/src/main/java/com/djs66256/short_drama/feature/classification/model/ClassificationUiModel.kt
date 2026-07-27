package com.djs66256.short_drama.feature.classification.model

import com.djs66256.short_drama.domain.model.ClassificationDimension
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationTagsPayload

data class ClassificationDimensionUiModel(
    val key: ClassificationDimensionKey,
    val title: String,
    val tags: List<String>,
    val emptyMessage: String = "当前维度暂无标签",
)

data class ClassificationUiModel(
    val dimensions: List<ClassificationDimensionUiModel>,
)

fun ClassificationTagsPayload.toUiModel(): ClassificationUiModel = ClassificationUiModel(
    dimensions = dimensions.map(ClassificationDimension::toUiModel),
)

private fun ClassificationDimension.toUiModel(): ClassificationDimensionUiModel = ClassificationDimensionUiModel(
    key = key,
    title = name,
    tags = tags,
)
