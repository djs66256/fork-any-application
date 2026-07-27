package com.djs66256.short_drama.domain.model

@Suppress("MagicNumber")
enum class ClassificationGender(
    val apiValue: String,
    val label: String,
) {
    ALL(apiValue = "all", label = "全部"),
    MALE(apiValue = "male", label = "男频"),
    FEMALE(apiValue = "female", label = "女频"),
    ;

    companion object {
        fun fromApiValue(value: String?): ClassificationGender = entries.firstOrNull {
            it.apiValue == value.orEmpty().trim()
        } ?: ALL
    }
}

enum class ClassificationDimensionKey(
    val apiValue: String,
    val label: String,
) {
    ERA_BACKGROUND(apiValue = "era_background", label = "时代背景"),
    THEME_PLOT(apiValue = "theme_plot", label = "主题情节"),
    CHARACTER_SETTING(apiValue = "character_setting", label = "角色设定"),
    ;

    companion object {
        fun fromApiValue(value: String?): ClassificationDimensionKey? = entries.firstOrNull {
            it.apiValue == value.orEmpty().trim()
        }
    }
}

data class ClassificationDimension(
    val key: ClassificationDimensionKey,
    val name: String,
    val tags: List<String>,
)

data class ClassificationTagsPayload(
    val gender: ClassificationGender,
    val dimensions: List<ClassificationDimension>,
)
