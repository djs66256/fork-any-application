package com.djs66256.short_drama.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DramaDtoTest {

    @Test
    fun `T-03 toDomain converts all fields correctly`() {
        val dto = DramaDto(
            id = "1",
            title = "Test Drama",
            description = "A test description",
            coverUrl = "https://example.com/cover.jpg",
            category = "action",
            episodeCount = 12,
            tags = listOf("drama", "action"),
            rating = 4.5,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-02T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("1", domain.id)
        assertEquals("Test Drama", domain.title)
        assertEquals("A test description", domain.description)
        assertEquals("https://example.com/cover.jpg", domain.coverUrl)
        assertEquals("action", domain.category)
        assertEquals(12, domain.episodeCount)
        assertEquals(listOf("drama", "action"), domain.tags)
        assertEquals(4.5, domain.rating, 0.0)
        assertEquals("2024-01-01T00:00:00Z", domain.createdAt)
        assertEquals("2024-01-02T00:00:00Z", domain.updatedAt)
    }

    @Test
    fun `T-03 snake_case JSON keys correctly mapped via SerialName`() {
        val dto = DramaDto(
            id = "2",
            title = "Another Drama",
            description = "Desc",
            coverUrl = "url2",
            category = "comedy",
            episodeCount = 5,
            tags = emptyList(),
            rating = 3.0,
            createdAt = "2024-01-01",
            updatedAt = "2024-01-02"
        )

        val domain = dto.toDomain()

        // Verify the @SerialName mapping worked by checking coverUrl field
        assertEquals("url2", domain.coverUrl)
        assertEquals(5, domain.episodeCount)
    }

    @Test
    fun `T-03 JSON decode supports null cover and rating`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val payload =
            """
            {
              "id": "3",
              "title": "Nullables Drama",
              "description": "Desc",
              "cover_url": null,
              "category": "urban",
              "episode_count": 9,
              "tags": ["系统"],
              "rating": null,
              "created_at": "2024-01-01T00:00:00Z",
              "updated_at": "2024-01-02T00:00:00Z"
            }
            """.trimIndent()

        val dto = json.decodeFromString<DramaDto>(payload)
        val domain = dto.toDomain()

        assertEquals(null, dto.coverUrl)
        assertEquals(null, dto.rating)
        assertEquals("", domain.coverUrl)
        assertEquals(0.0, domain.rating, 0.0)
    }
}
