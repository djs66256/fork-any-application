package com.djs66256.short_drama.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionStoreTest {

    @Test
    fun `T-02 getOrCreateSessionId generates uuid once and reuses persisted value`() = runTest {
        val tempFile = File.createTempFile("playback-session", ".preferences_pb")
        tempFile.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFile },
        )

        val store = DataStorePlaybackSessionStore(dataStore)

        val first = store.getOrCreateSessionId()
        val second = store.getOrCreateSessionId()

        assertEquals(first, second)
        assertTrue(first.matches(UUID_REGEX))
    }

    private companion object {
        val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }
}
