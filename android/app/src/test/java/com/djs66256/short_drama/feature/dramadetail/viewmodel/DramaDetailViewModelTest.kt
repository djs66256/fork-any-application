package com.djs66256.short_drama.feature.dramadetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class DramaDetailViewModelTest {

    @Test
    fun `dramaId extracted from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle(mapOf("dramaId" to "456"))
        val viewModel = DramaDetailViewModel(savedStateHandle)
        assertEquals("456", viewModel.dramaId)
    }

    @Test
    fun `dramaId falls back to generic id`() {
        val savedStateHandle = SavedStateHandle(mapOf("id" to "fallback-456"))
        val viewModel = DramaDetailViewModel(savedStateHandle)
        assertEquals("fallback-456", viewModel.dramaId)
    }

    @Test
    fun `dramaId defaults to empty string when missing`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = DramaDetailViewModel(savedStateHandle)
        assertEquals("", viewModel.dramaId)
    }
}
