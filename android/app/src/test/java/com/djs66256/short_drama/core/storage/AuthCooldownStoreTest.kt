package com.djs66256.short_drama.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthCooldownStoreTest {

    @Test
    fun `T-01 cooldown deadline can be written read and cleared`() = runTest {
        val tempFile = File.createTempFile("auth-cooldown", ".preferences_pb")
        tempFile.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFile },
        )
        val store = AuthCooldownStore(dataStore)

        store.write(1234L)
        assertEquals(1234L, store.read())

        store.clear()
        assertEquals(null, store.read())
    }
}
