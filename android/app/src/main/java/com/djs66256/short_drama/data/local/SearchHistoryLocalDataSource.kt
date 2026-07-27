package com.djs66256.short_drama.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class SearchHistoryLocalDataSource @Inject constructor(
    @Named("searchHistory") private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    val history: Flow<List<SearchHistoryItem>> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { preferences ->
            decodeSearchHistory(preferences[SEARCH_HISTORY_KEY], json)
        }

    suspend fun save(keyword: String) {
        val normalizedKeyword = normalizeSearchQueryOrNull(keyword) ?: return
        dataStore.edit { preferences ->
            val currentItems = decodeSearchHistory(preferences[SEARCH_HISTORY_KEY], json)
            val updatedItems = mergeSearchHistory(
                currentItems = currentItems,
                newKeyword = normalizedKeyword,
                nowEpochMillis = System.currentTimeMillis(),
            )
            preferences[SEARCH_HISTORY_KEY] = encodeSearchHistory(updatedItems, json)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences[SEARCH_HISTORY_KEY] = encodeSearchHistory(emptyList(), json)
        }
    }

    private companion object {
        private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history_entries")
    }
}

internal fun mergeSearchHistory(
    currentItems: List<SearchHistoryItem>,
    newKeyword: String,
    nowEpochMillis: Long,
): List<SearchHistoryItem> {
    val normalizedKeyword = normalizeSearchQueryOrNull(newKeyword) ?: return currentItems
    return listOf(
        SearchHistoryItem(
            keyword = normalizedKeyword,
            updatedAtEpochMillis = nowEpochMillis,
        ),
    ) + currentItems
        .mapNotNull { item ->
            normalizeSearchQueryOrNull(item.keyword)?.let { normalizedExistingKeyword ->
                SearchHistoryItem(
                    keyword = normalizedExistingKeyword,
                    updatedAtEpochMillis = item.updatedAtEpochMillis,
                )
            }
        }
        .filterNot { it.keyword == normalizedKeyword }
        .sortedByDescending(SearchHistoryItem::updatedAtEpochMillis)
        .take(MAX_SEARCH_HISTORY_COUNT - 1)
}

internal fun decodeSearchHistory(serialized: String?, json: Json): List<SearchHistoryItem> {
    val records = runCatching {
        json.decodeFromString<List<SearchHistoryRecord>>(serialized.orEmpty().ifBlank { "[]" })
    }.getOrDefault(emptyList())

    return records
        .mapNotNull { record ->
            normalizeSearchQueryOrNull(record.keyword)?.let { normalizedKeyword ->
                SearchHistoryItem(
                    keyword = normalizedKeyword,
                    updatedAtEpochMillis = record.updatedAtEpochMillis,
                )
            }
        }
        .sortedByDescending(SearchHistoryItem::updatedAtEpochMillis)
        .distinctBy(SearchHistoryItem::keyword)
        .take(MAX_SEARCH_HISTORY_COUNT)
}

internal fun encodeSearchHistory(items: List<SearchHistoryItem>, json: Json): String {
    val records = items
        .mapNotNull { item ->
            normalizeSearchQueryOrNull(item.keyword)?.let { normalizedKeyword ->
                SearchHistoryRecord(
                    keyword = normalizedKeyword,
                    updatedAtEpochMillis = item.updatedAtEpochMillis,
                )
            }
        }
        .sortedByDescending(SearchHistoryRecord::updatedAtEpochMillis)
        .distinctBy(SearchHistoryRecord::keyword)
        .take(MAX_SEARCH_HISTORY_COUNT)

    return json.encodeToString(records)
}

@Serializable
internal data class SearchHistoryRecord(
    val keyword: String,
    val updatedAtEpochMillis: Long,
)

internal const val MAX_SEARCH_HISTORY_COUNT = 10
