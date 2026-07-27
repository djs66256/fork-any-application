package com.djs66256.short_drama.feature.search.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchComponentsTest {
    @Test
    fun `T-07 canSubmitSearch validates trimmed non empty queries`() {
        assertTrue(canSubmitSearch("逆袭"))
        assertTrue(canSubmitSearch(" 逆袭 "))
        assertFalse(canSubmitSearch(""))
        assertFalse(canSubmitSearch("   "))
    }
}
