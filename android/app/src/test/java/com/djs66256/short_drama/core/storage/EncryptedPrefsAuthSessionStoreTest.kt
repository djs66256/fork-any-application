package com.djs66256.short_drama.core.storage

import android.content.SharedPreferences
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncryptedPrefsAuthSessionStoreTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `T-01 write and read should keep auth session consistent`() = runTest {
        val sharedPreferences = InMemorySharedPreferences()
        val store = EncryptedPrefsAuthSessionStore(sharedPreferences, json)
        val session = sampleSession()

        store.write(session)

        assertEquals(session, store.read())
    }

    @Test
    fun `T-01 malformed payload should be cleared and return null`() = runTest {
        val sharedPreferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPreferences.getString(any(), any()) } returns "not-json"
        every { sharedPreferences.contains(any()) } returns true
        every { sharedPreferences.edit() } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit

        val store = EncryptedPrefsAuthSessionStore(sharedPreferences, json)

        assertNull(store.read())
        verify { editor.remove("auth_session_payload") }
        verify { editor.apply() }
    }

    private fun sampleSession(): AuthSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-28T12:00:00Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = "测试用户",
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = true,
        ),
    )
}

private class InMemorySharedPreferences : SharedPreferences {
    private val data = linkedMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues

    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor(data)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private class Editor(
        private val backing: MutableMap<String, Any?>,
    ) : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = null
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                backing.clear()
            }
            pending.forEach { (key, value) ->
                if (value == null) {
                    backing.remove(key)
                } else {
                    backing[key] = value
                }
            }
            pending.clear()
            clearRequested = false
        }
    }
}
