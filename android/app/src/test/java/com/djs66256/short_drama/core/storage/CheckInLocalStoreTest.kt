package com.djs66256.short_drama.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInLocalStoreTest {

    @Test
    fun `T-02 local store generates installation id once and stores dismissed server date`() = runTest {
        val tempFile = File.createTempFile("check-in-store", ".preferences_pb")
        tempFile.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFile },
        )

        val store = DataStoreCheckInLocalStore(dataStore)

        val firstInstallationId = store.getOrCreateInstallationId()
        val secondInstallationId = store.getOrCreateInstallationId()
        store.setDismissedServerDate("2026-07-29")

        assertEquals(firstInstallationId, secondInstallationId)
        assertTrue(firstInstallationId.matches(UUID_REGEX))
        assertEquals("2026-07-29", store.getDismissedServerDate())
    }

    private companion object {
        val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }
}
